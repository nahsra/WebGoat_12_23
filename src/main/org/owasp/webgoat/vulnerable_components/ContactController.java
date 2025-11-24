package org.owasp.webgoat.vulnerable_components;

import org.owasp.webgoat.LessonDataSource;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;
import java.sql.*;
import java.security.MessageDigest;

/** Handle contact management with two new endpoints. */
@RestController
public abstract class ContactController {

    private final LessonDataSource dataSource;
    
    public ContactController(LessonDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @RequestMapping(path = "/search", method = {RequestMethod.GET, RequestMethod.POST})
    public @ResponseBody 
    String search(String q) throws SQLException {
      return contactDao.search(q);
    }

    @RequestMapping(path = "/updatePw", method = {RequestMethod.POST})
    public @ResponseBody 
    String updatePassword(String newPassword1, String newPassword2) throws SQLException {
      if(!newPassword1.equals(newPassword2)) { 
          return "nomatch"; 
      }
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] b = md.digest(newPassword.getBytes("UTF-8"));
      String hexMd5 = toHexString(b);
      return userDao.updatePassword(getContext(), hexMd5);
    }

    @RequestMapping("/search")
    public @ResponseBody
    void logout() throws SQLException {
        if (currentSession().isPresent()) {
            Session session = currentSession().get();
            session.logout();
        }
    }

    // different controllers will have different session resolvers
    protected abstract Optional<Session> currentSession();
}
