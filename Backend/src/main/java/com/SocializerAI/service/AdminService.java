package com.SocializerAI.service;

import org.springframework.stereotype.Service;
import com.SocializerAI.repository.AdminRepository;
import com.SocializerAI.model.AdminUser;
import java.util.UUID;

@Service
public class AdminService {
    private final AdminRepository repo;

    public AdminService(AdminRepository repo){ this.repo = repo; }

    public AdminUser create(AdminUser u){ return repo.save(u); }
    public AdminUser get(UUID id){ return repo.findById(id).orElseThrow(); }
    public void deactivate(UUID id){
        var u = repo.findById(id).orElseThrow();
        u.setActive(false);
        repo.save(u);
    }
}
