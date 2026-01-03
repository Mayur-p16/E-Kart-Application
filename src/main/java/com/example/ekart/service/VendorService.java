package com.example.ekart.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;

import com.example.ekart.dto.Product;
import com.example.ekart.dto.Vendor;
import com.example.ekart.helper.AES;
import com.example.ekart.helper.CloudinaryHelper;
import com.example.ekart.helper.EmailSender;
import com.example.ekart.repository.ProductRepository;
import com.example.ekart.repository.VendorRepository;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Service
public class VendorService {
	@Autowired
	ProductRepository productRepository;
	
	@Autowired
	CloudinaryHelper cloudinaryHelper;
	
	@Autowired
	VendorRepository vendorRepository;

	@Autowired
	EmailSender emailSender;

	public String loadRegistration(ModelMap map, Vendor vendor) {
		map.put("vendor", vendor);
		return "vendor-register.html";
	}

	public String registration(@Valid Vendor vendor, BindingResult result, HttpSession session) {
		if (!vendor.getPassword().equals(vendor.getConfirmPassword()))
			result.rejectValue("confirmPassword", "error.confirmPassword",
					"* password and confirmPassword should match");

		if (vendorRepository.existsByEmail(vendor.getEmail()))
			result.rejectValue("email", "error.email", "* email already exists");

		if (vendorRepository.existsByMobile(vendor.getMobile()))
			result.rejectValue("mobile", "error.mobile", "* mobile number already exists");

		if (result.hasErrors())
			return "vendor-register.html";
		else {
			int otp = new Random().nextInt(100000, 1000000);
			vendor.setOtp(otp);
			vendor.setPassword(AES.encrypt(vendor.getPassword()));
			vendorRepository.save(vendor);
			// email logic
			// emailSender.send(vendor);
			System.err.println(vendor);
			session.setAttribute("success", "Otp Sent Successfully");
			return "redirect:/vendor/otp/" + vendor.getId();
		}
	}

	public String verifyOtp(int id, int otp, HttpSession session) {
		Vendor vendor = vendorRepository.findById(id).orElseThrow();
		if (vendor.getOtp() == otp) {
			vendor.setVerified(true);
			vendorRepository.save(vendor);
			session.setAttribute("success", "Vendor Account Created Success");
			return "redirect:/";
		} else {
			session.setAttribute("failure", "OTP Mismatch");
			return "redirect:/vendor/otp/" + vendor.getId();
		}
	}

	public String login(String email, String password, HttpSession session) {
		Vendor vendor = vendorRepository.findByEmail(email);
		if (vendor == null) {
			session.setAttribute("failure", "Invalid Email");
			return "redirect:/vendor/login";
		} else {
			if (AES.decrypt(vendor.getPassword()).equals(password)) {
				if (vendor.isVerified()) {
					session.setAttribute("vendor", vendor);
					session.setAttribute("success", "Login Success");
					return "redirect:/vendor/home";
				} else {
					int otp = new Random().nextInt(100000, 1000000);
					vendor.setOtp(otp);
					vendorRepository.save(vendor);
					// emailSender.send(vendor);
					System.err.println(vendor.getOtp());
					session.setAttribute("success", "Otp Sent Successfully, First Verify Email for Logging In");
					return "redirect:/vendor/otp/" + vendor.getId();
				}
			} else {
				session.setAttribute("failure", "Invalid Password");
				return "redirect:/vendor/login";
			}
		}
	}

	public String loadOtpPage(int id, ModelMap map) {
		map.put("id", id);
		return "vendor-otp.html";
	}

	public String loadHome(HttpSession session) {
		if (session.getAttribute("vendor") != null)
			return "vendor-home.html";
		else {
			session.setAttribute("failure", "Invalid Session, First Login");
			return "redirect:/vendor/login";
		}
	}

	public String laodAddProduct(HttpSession session) {
		if (session.getAttribute("vendor") != null)
			return "add-product.html";
		else {
			session.setAttribute("failure", "Invalid Session, First Login");
			return "redirect:/vendor/login";
		}
	}

	public String laodAddProduct(Product product, HttpSession session) throws IOException {
		if (session.getAttribute("vendor") != null) {
			Vendor vendor = (Vendor) session.getAttribute("vendor");
			product.setVendor(vendor);
			product.setImageLink(cloudinaryHelper.saveToCloudinary(product.getImage()));
			productRepository.save(product);
			session.setAttribute("success", "Product Added Success");
			return "redirect:/vendor/home";
		} else {
			session.setAttribute("failure", "Invalid Session, First Login");
			return "redirect:/vendor/login";
		}
	}

	public String manageProducts(HttpSession session, ModelMap map) {
		if (session.getAttribute("vendor") != null) {
			Vendor vendor = (Vendor) session.getAttribute("vendor");
			List<Product> products = productRepository.findByVendor(vendor);
			if (products.isEmpty()) {
				session.setAttribute("failure", "No Products Present");
				return "redirect:/vendor/home";
			} else {
				map.put("products", products);
				return "vendor-view-products.html";
			}
		} else {
			session.setAttribute("failure", "Invalid Session, First Login");
			return "redirect:/vendor/login";
		}
	}

	public String delete(int id, HttpSession session) {
		if (session.getAttribute("vendor") != null) {
			productRepository.deleteById(id);
			session.setAttribute("success", "Product Deleted Success");
			return "redirect:/manage-products";
		} else {
			session.setAttribute("failure", "Invalid Session, First Login");
			return "redirect:/vendor/login";
		}
	}

	public String editProduct(int id, ModelMap map, HttpSession session) {
		if (session.getAttribute("vendor") != null) {
			Product product = productRepository.findById(id).get();
			map.put("product", product);
			return "edit-product.html";
		} else {
			session.setAttribute("failure", "Invalid Session, First Login");
			return "redirect:/vendor/login";
		}
	}

	public String updateProduct(Product product, HttpSession session) throws IOException {
		if (session.getAttribute("vendor") != null) {
			Vendor vendor = (Vendor) session.getAttribute("vendor");
			product.setImageLink(cloudinaryHelper.saveToCloudinary(product.getImage()));
			product.setVendor(vendor);
			productRepository.save(product);
			session.setAttribute("success", "Product Updated Success");
			return "redirect:/manage-products";
		} else {
			session.setAttribute("failure", "Invalid Session, First Login");
			return "redirect:/vendor/login";
		}
	}

}