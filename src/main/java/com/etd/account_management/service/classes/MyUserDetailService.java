package com.etd.account_management.service.classes;

import com.etd.account_management.dao.EmployeeRepo;
import com.etd.account_management.entity.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MyUserDetailService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(MyUserDetailService.class);

    private final EmployeeRepo employeeRepo;

    public MyUserDetailService(EmployeeRepo employeeRepo) {
        this.employeeRepo = employeeRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.info("Inside MyUserDetailService :: loadUserByUsername : {}", email);
        Employee employee = employeeRepo.findByEmailAddress(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return new User(
                employee.getEmailAddress(),
                employee.getPassword(),
                Boolean.TRUE.equals(employee.getAccessGranted()),
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority(employee.getRole())));
    }

}
