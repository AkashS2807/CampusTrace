package com.example.campustrace.controller;

import com.example.campustrace.model.Item;
import com.example.campustrace.repository.ItemRepository;
import com.example.campustrace.repository.UserRepository;
import com.example.campustrace.web.ItemForm;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {
    private final ItemRepository items;
    private final UserRepository users;
    public HomeController(ItemRepository items, UserRepository users) { this.items=items; this.users=users; }
    @GetMapping("/") public String home() { return "index"; }
    @ModelAttribute
    void userContext(Authentication auth, Model model) {
        if (auth == null) { return; }
        var user=users.findByEmailIgnoreCase(auth.getName());
        model.addAttribute("name",user == null ? auth.getName() : user.getFullName());
        model.addAttribute("userEmail",auth.getName()); model.addAttribute("admin",isAdmin(auth));
    }
    @GetMapping("/dashboard") public String dashboard(Model model) {
        model.addAttribute("totalItems",items.count());
        model.addAttribute("openItems",items.countByStatusIgnoreCase("OPEN")); return "dashboard";
    }
    @GetMapping({"/lostItem","/foundItem"})
    public String reportPage(jakarta.servlet.http.HttpServletRequest request, Model model) {
        model.addAttribute("form",new ItemForm());
        return request.getRequestURI().equals(request.getContextPath()+"/lostItem") ? "lostItem" : "foundItem";
    }
    @PostMapping("/saveLostItem")
    public String lost(@Valid @ModelAttribute("form") ItemForm form, BindingResult errors,
            Authentication auth, Model model, RedirectAttributes flash) {
        return save(form,errors,auth,model,flash,"LOST","lostItem");
    }
    @PostMapping("/saveFoundItem")
    public String found(@Valid @ModelAttribute("form") ItemForm form, BindingResult errors,
            Authentication auth, Model model, RedirectAttributes flash) {
        return save(form,errors,auth,model,flash,"FOUND","foundItem");
    }
    private String save(ItemForm form, BindingResult errors, Authentication auth, Model model,
            RedirectAttributes flash, String type, String view) {
        if (errors.hasErrors()) {
            model.addAttribute("error","Enter an item name (up to 100 characters), description (up to 255), and valid phone number.");
            return view;
        }
        var user=users.findByEmailIgnoreCase(auth.getName());
        if (user == null) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED); }
        Item item=new Item(); item.setItemName(form.itemName.trim()); item.setDescription(form.description.trim());
        item.setContactNumber(form.contactNumber.trim()); item.setLocation(form.location == null ? "" : form.location.trim());
        item.setType(type); item.setStatus("OPEN"); item.setPostedBy(user.getEmail()); item.setPostedByName(user.getFullName());
        items.save(item); flash.addFlashAttribute("success","Item report submitted."); return "redirect:/items";
    }
    @GetMapping({"/items","/search","/myPosts"})
    public String listings(@RequestParam(defaultValue="") String keyword,
            @RequestParam(defaultValue="") String type, @RequestParam(defaultValue="") String status,
            jakarta.servlet.http.HttpServletRequest request, Authentication auth, Model model) {
        type=filter(type,List.of("LOST","FOUND")); status=filter(status,List.of("OPEN","RESOLVED"));
        keyword=keyword.trim();
        if (keyword.length()>100) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST); }
        boolean own=request.getRequestURI().equals(request.getContextPath()+"/myPosts");
        model.addAttribute("items",items.search(keyword,type,status,own ? auth.getName() : ""));
        model.addAttribute("keyword",keyword); model.addAttribute("selectedType",type); model.addAttribute("selectedStatus",status);
        model.addAttribute("listPath",own ? "/myPosts" : "/items");
        model.addAttribute("pageTitle",own ? "My Items" : "All Items"); return "items";
    }
    @PostMapping("/resolve/{id}") @Transactional
    public String resolve(@PathVariable Long id, Authentication auth, RedirectAttributes flash) {
        Item item=items.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!isAdmin(auth) && !auth.getName().equals(item.getPostedBy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (!"OPEN".equals(item.getStatus()) && !"RESOLVED".equals(item.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        item.setStatus("RESOLVED"); items.save(item);
        flash.addFlashAttribute("success","Item marked as resolved."); return "redirect:/items";
    }
    @GetMapping("/allUsers") public String allUsers(Model model) {
        model.addAttribute("users",users.findAll()); return "users";
    }
    @GetMapping("/manageItems") public String manage(Model model) {
        model.addAttribute("items",items.findAll()); return "manageItems";
    }
    @PostMapping("/deleteItem/{id}") @Transactional
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        Item item=items.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        items.delete(item); flash.addFlashAttribute("success","Item deleted."); return "redirect:/manageItems";
    }
    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
    private String filter(String value,List<String> permitted) {
        value=value.trim().toUpperCase(Locale.ROOT);
        if (!value.isEmpty() && !permitted.contains(value)) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST); }
        return value;
    }
}
