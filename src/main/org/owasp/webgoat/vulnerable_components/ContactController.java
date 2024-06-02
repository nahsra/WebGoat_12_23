package org.owasp.webgoat.vulnerable_components;

import org.owasp.webgoat.LessonDataSource;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

import java.sql.*;

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
      // get the phone number from the database
      return contactDao.search();
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
