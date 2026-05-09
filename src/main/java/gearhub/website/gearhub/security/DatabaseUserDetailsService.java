package gearhub.website.gearhub.security;

import gearhub.website.gearhub.model.User;
import gearhub.website.gearhub.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String subject) throws UsernameNotFoundException {
        User user;
        if (subject != null && subject.matches("\\d+")) {
            user = userRepository.findById(Long.parseLong(subject))
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        } else {
            user = userRepository.findByEmailIgnoreCase(subject)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        }
        return new SecurityUser(user);
    }
}
