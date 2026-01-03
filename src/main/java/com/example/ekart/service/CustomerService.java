package com.example.ekart.service;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;

import com.example.ekart.dto.Cart;
import com.example.ekart.dto.Customer;
import com.example.ekart.dto.Item;
import com.example.ekart.dto.Product;
import com.example.ekart.helper.AES;
import com.example.ekart.helper.EmailSender;
import com.example.ekart.repository.CustomerRepository;
import com.example.ekart.repository.ItemRepository;
import com.example.ekart.repository.OrderRepository;
import com.example.ekart.repository.ProductRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import jakarta.servlet.http.HttpSession;

@Service
public class CustomerService {

	@Autowired
	OrderRepository orderRepository;
	
	@Autowired
	CustomerRepository customerRepository;
	
	@Autowired
	ProductRepository productRepository;
	
	@Autowired
	EmailSender emailSender;
	
	@Autowired
	ItemRepository itemRepository;
	
	
	public String loadRegistration(ModelMap map, Customer customer) {
		map.put("customer", customer);
		return "customer-register.html";
	}

	public String registration(Customer customer, BindingResult result,HttpSession session) {
		if (!customer.getPassword().equals(customer.getConfirmPassword()))
			result.rejectValue("confirmPassword", "error.confirmPassword",
					"* Password and Confirm Password Should Match");
		if (customerRepository.existsByEmail(customer.getEmail()))
			result.rejectValue("email", "error.email", "* Email Already Exists");
		if (customerRepository.existsByMobile(customer.getMobile()))
			result.rejectValue("mobile", "error.mobile", "* Mobile Number Already Exists");

		if (result.hasErrors())
			return "customer-register.html";
		else {
			int otp = new Random().nextInt(100000, 1000000);
			customer.setOtp(otp);
			customer.setPassword(AES.encrypt(customer.getPassword()));
			customerRepository.save(customer);
			//emailSender.send(vendor);
			System.err.println(customer.getOtp());
			session.setAttribute("success", "Otp Sent Successfully");
			return "redirect:/customer/otp/" + customer.getId();
		}
	}

	public String verifyOtp(int id, int otp,HttpSession session) {
		Customer customer = customerRepository.findById(id).orElseThrow();
		if (customer.getOtp() == otp) {
			customer.setVerified(true);
			customerRepository.save(customer);
			session.setAttribute("success", "Customer Account Created Success");
			return "redirect:/";
		} else {
			session.setAttribute("failure", "OTP Missmatch");
			return "redirect:/customer/otp/" + customer.getId();
		}
	}

	public String login(String email, String password, HttpSession session) {
		Customer customer=customerRepository.findByEmail(email);
		if(customer==null) {
			session.setAttribute("failure", "Invalid Email");
			return "redirect:/customer/login";
		}else {
			if(AES.decrypt(customer.getPassword()).equals(password)) {
				if(customer.isVerified()) {
					session.setAttribute("customer", customer);
					session.setAttribute("success", "Login Success");
					return "redirect:/customer/home";
				}else {
					int otp = new Random().nextInt(100000, 1000000);
					customer.setOtp(otp);
					customerRepository.save(customer);
					//emailSender.send(vendor);
					System.err.println(customer.getOtp());
					session.setAttribute("success", "Otp Sent Successfully, First Verify Email for Logging In");
					return "redirect:/customer/otp/" + customer.getId();
				}
			}else {
				session.setAttribute("failure", "Invalid Password");
				return "redirect:/customer/login";
			}
		}
	}

	public String loadCustomerHome(HttpSession session) {
		if (session.getAttribute("customer") != null)
			return "customer-home.html";
		else {
			session.setAttribute("failure", "Invalid Session, First Login");
			return "redirect:/customer/login";
		}
	}

	public String viewProducts(HttpSession session, ModelMap map) {
		if (session.getAttribute("customer") != null) {
			List<Product> products = productRepository.findByApprovedTrue();
			if (products.isEmpty()) {
				session.setAttribute("failure", "No Products Present");
				return "redirect:/customer/home";
			} else {
				map.put("products", products);
				return "customer-view-products.html";
			}
		} else {
			session.setAttribute("failure", "Invalid Session, First Login");
			return "redirect:/customer/login";
		}
	}

	public String searchProducts(HttpSession session) {
		if (session.getAttribute("customer") != null) {
			return "search.html";
		} else {
			session.setAttribute("failure", "Invalid Session, First Login");
			return "redirect:/customer/login";
		}
	}

	public String search(String query, HttpSession session, ModelMap map) {
		if (session.getAttribute("customer") != null) {
			String toSearch = "%" + query + "%";
			List<Product> list1 = productRepository.findByNameLike(toSearch);
			List<Product> list2 = productRepository.findByDescriptionLike(toSearch);
			List<Product> list3 = productRepository.findByCategoryLike(toSearch);
			HashSet<Product> products = new HashSet<Product>();
			products.addAll(list1);
			products.addAll(list2);
			products.addAll(list3);
			map.put("products", products);
			map.put("query", query);
			return "search.html";
		} else {
			session.setAttribute("failure", "Invalid Session, First Login");
			return "redirect:/customer/login";
		}
	}

	public String increase(int id, HttpSession session) {
		if (session.getAttribute("customer") != null) {
			Customer customer = (Customer) session.getAttribute("customer");
			Item item = itemRepository.findById(id).get();
			Product product = productRepository.findByNameLike(item.getName()).get(0);
			if (product.getStock() == 0) {
				session.setAttribute("failure", "Sorry! Product Out of Stock");
				return "redirect:/view-cart";
			} else {
				item.setQuantity(item.getQuantity() + 1);
				item.setPrice(item.getPrice() + product.getPrice());
				itemRepository.save(item);
				product.setStock(product.getStock() - 1);
				productRepository.save(product);
				session.setAttribute("success", "Product Added to Cart Success");
				session.setAttribute("customer", customerRepository.findById(customer.getId()).get());
				return "redirect:/view-cart";
			}
		} else {
			session.setAttribute("failure", "Invalid Session, First Login");
			return "redirect:/customer/login";
		}
	}
	
	public String decrease(int id, HttpSession session) {
		if (session.getAttribute("customer") != null) {
			Customer customer = (Customer) session.getAttribute("customer");
			Item item = itemRepository.findById(id).get();
			Product product = productRepository.findByNameLike(item.getName()).get(0);

			if (item.getQuantity() > 1) {
				item.setQuantity(item.getQuantity() - 1);
				item.setPrice(item.getPrice() - product.getPrice());
				itemRepository.save(item);
				product.setStock(product.getStock() + 1);
				productRepository.save(product);
				session.setAttribute("success", "Product Removed from Cart Success");
				session.setAttribute("customer", customerRepository.findById(customer.getId()).get());
				return "redirect:/view-cart";
			} else {
				customer.getCart().getItems().remove(item);
				customerRepository.save(customer);
				session.setAttribute("success", "Product Quantity Reduced from Cart Success");
				session.setAttribute("customer", customerRepository.findById(customer.getId()).get());
				return "redirect:/view-cart";
			}
		} else {
			session.setAttribute("failure", "Invalid Session, First Login");
			return "redirect:/customer/login";
		}
	}

	public String payment(HttpSession session, ModelMap map) {
		Customer customer = (Customer) session.getAttribute("customer");
		if (session.getAttribute("customer") != null) {
			try {
				double amount = customer.getCart().getItems().stream().mapToDouble(i -> i.getPrice()).sum();
				RazorpayClient client = new RazorpayClient("rzp_test_zH7QtiK5JnMMiw", "HWQWwpKQbK5XdLZEUTqnGQVC");
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("currency", "INR");
				jsonObject.put("amount", amount * 100);
				Order order = client.orders.create(jsonObject);
				map.put("key", "rzp_test_zH7QtiK5JnMMiw");
				map.put("id", order.get("id"));
				map.put("amount", amount * 100);
				map.put("customer", customer);
				return "payment.html";
			} catch (RazorpayException e) {
				session.setAttribute("failure", "Invalid Session, First Login");
				return "redirect:/customer/login";
			}
		} else {
			session.setAttribute("failure", "Invalid Session, First Login");
			return "redirect:/customer/login";
		}
	}

	public String paymentSuccess(com.example.ekart.dto.Order order, HttpSession session) {
		if (session.getAttribute("customer") != null) {
			Customer customer = (Customer) session.getAttribute("customer");
			order.setCustomer(customer);
			order.setTotalPrice(customer.getCart().getItems().stream().mapToDouble(i -> i.getPrice()).sum());
			List<Item> items = customer.getCart().getItems();
			System.out.println(items.size());

			List<Item> orderItems = order.getItems();
			for (Item item : items) {
				Item item2 = new Item();
				item2.setCategory(item.getCategory());
				item2.setDescription(item.getDescription());
				item2.setImageLink(item.getImageLink());
				item2.setName(item.getName());
				item2.setPrice(item.getPrice());
				item2.setQuantity(item.getQuantity());
				orderItems.add(item2);
			}
			
			order.setItems(orderItems);
			orderRepository.save(order);

			customer.getCart().getItems().clear();
			customerRepository.save(customer);

			session.setAttribute("customer", customerRepository.findById(customer.getId()).get());
			session.setAttribute("success", "Order Placed Success");
			return "redirect:/customer/home";
		} else {
			session.setAttribute("failure", "Invalid Session, First Login");
			return "redirect:/customer/login";
		}
	}

	public String viewOrders(HttpSession session, ModelMap map) {
		if (session.getAttribute("customer") != null) {
			Customer customer = (Customer) session.getAttribute("customer");
			List<com.example.ekart.dto.Order> orders = orderRepository.findByCustomer(customer);
			if (orders.isEmpty()) {
				session.setAttribute("success", "No Orders Placed Yet");
				return "redirect:/customer/home";
			} else {
				map.put("orders", orders);
				return "view-orders.html";
			}
		} else {
			session.setAttribute("failure", "Invalid Session, First Login");
			return "redirect:/customer/login";
		}
	}

	public String viewCart(HttpSession session, ModelMap map) {
		if (session.getAttribute("customer") != null) {
			Customer customer = (Customer) session.getAttribute("customer");
			Cart cart = customer.getCart();
			if (cart == null) {
				session.setAttribute("failure", "Nothing is Present inside Cart");
				return "redirect:/customer/home";
			} else {
				List<Item> items = cart.getItems();
				if (items.isEmpty()) {
					session.setAttribute("failure", "Nothing is Present inside Cart");
					return "redirect:/customer/home";
				} else {
					map.put("totalPrice", items.stream().mapToDouble(i->i.getPrice()).sum());
					map.put("items", items);
					return "view-cart.html";
				}
			}
		} else {
			session.setAttribute("failure", "Invalid Session, First Login");
			return "redirect:/customer/login";
		}
	}

	public String addToCart(int id, HttpSession session) {
		if (session.getAttribute("customer") != null) {
			Product product = productRepository.findById(id).get();
			if (product.getStock() > 0) {
				Customer customer = (Customer) session.getAttribute("customer");
				Cart cart = customer.getCart();
				List<Item> items = cart.getItems();
				if (items.stream().map(x -> x.getName()).collect(Collectors.toList()).contains(product.getName())) {
					session.setAttribute("failure", "Product Already Exists in Cart");
					return "redirect:/customer/home";
				} else {
					Item item = new Item();
					item.setName(product.getName());
					item.setCategory(product.getCategory());
					item.setDescription(product.getDescription());
					item.setImageLink(product.getImageLink());
					item.setPrice(product.getPrice());
					item.setQuantity(1);
					items.add(item);

					customerRepository.save(customer);
					session.setAttribute("success", "Product Added to Cart Success");
					session.setAttribute("customer", customerRepository.findById(customer.getId()).get());
					return "redirect:/customer/home";
				}
			} else {
				session.setAttribute("failure", "Sorry! Product Out of Stock");
				return "redirect:/customer/home";
			}
		} else {
			session.setAttribute("failure", "Invalid Session, First Login");
			return "redirect:/customer/login";
		}
	}

	

}