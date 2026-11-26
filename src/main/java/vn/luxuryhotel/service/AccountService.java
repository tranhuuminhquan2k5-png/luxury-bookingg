package vn.luxuryhotel.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.luxuryhotel.dto.Forms;
import vn.luxuryhotel.entity.*;
import vn.luxuryhotel.repository.*;
import vn.luxuryhotel.configuration.HotelTime;
import vn.luxuryhotel.exception.BusinessException;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class AccountService {
    private final UserRepository users;
    private final HotelRepository db;
    private final PasswordEncoder encoder;
    private final HotelTime time;
    private final ActorService actors;
    public UserAccount register(Forms.Register form){
        String email=form.getEmail().trim().toLowerCase(Locale.ROOT);
        if(users.existsByEmail(email))throw new BusinessException("Email đã được sử dụng.");
        var u=new UserAccount();u.setEmail(email);u.setPassword(encoder.encode(form.getPassword()));
        u.setFullName(form.getFullName().trim());u.setPhone(form.getPhone());u.setCreatedAt(time.now());
        u.setRole(db.list("from Role where name='CUSTOMER'",Role.class,Map.of()).getFirst());
        users.saveAndFlush(u);audit(u,"REGISTER","Đăng ký tài khoản");return u;
    }
    public void profile(Forms.Profile form){
        var u=actors.current();u.setFullName(form.getFullName().trim());u.setPhone(form.getPhone());u.setAddress(form.getAddress());
        users.save(u);audit(u,"PROFILE","Cập nhật hồ sơ");
    }
    public void updateUser(Forms.UserEdit form){
        var actor=actors.admin();db.lock(SystemLock.class,1L);
        var u=db.lock(UserAccount.class,form.getId());
        if(u.isEnabled()&&u.getRole().getName().equals("ADMIN")&&(!form.isEnabled()||!form.getRole().equals("ADMIN"))
            &&users.countByRoleNameAndEnabledTrue("ADMIN")<=1)throw new BusinessException("Phải giữ ít nhất một quản trị viên hoạt động.");
        var roles=db.list("from Role where name=:name",Role.class,Map.of("name",form.getRole()));
        if(roles.isEmpty())throw new BusinessException("Vai trò không hợp lệ.");
        u.setRole(roles.getFirst());u.setEnabled(form.isEnabled());
        audit(actor,"USER_ACCESS","Tài khoản #"+u.getId()+" → "+form.getRole()+", hoạt động: "+form.isEnabled());
    }
    public void audit(UserAccount actor,String action,String detail){
        var log=new AuditLog();log.setActor(actor);log.setAction(action);log.setDetail(detail);log.setCreatedAt(time.now());db.add(log);
    }
}
