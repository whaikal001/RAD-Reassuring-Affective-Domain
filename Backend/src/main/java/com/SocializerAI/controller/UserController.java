package com.SocializerAI.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.SocializerAI.model.User;
import com.SocializerAI.dto.AccountUpdateRequest;
import com.SocializerAI.service.UserService;
import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService svc;

    public UserController(UserService svc){ this.svc = svc; }

    @GetMapping public List<User> getAll(){ return svc.getAll(); }
    @GetMapping("/{id}") public User get(@PathVariable("id") UUID id){ return svc.get(id); }
    @PutMapping("/{id}") public User update(@PathVariable("id") UUID id, @RequestBody User u){ return svc.update(id,u); }
    @PutMapping("/{id}/account")
    public User updateAccount(@PathVariable("id") UUID id, @RequestBody AccountUpdateRequest req){
        try {
            return svc.updateAccount(id, req);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
    @DeleteMapping("/{id}") public void delete(@PathVariable("id") UUID id){ svc.delete(id); }
}
