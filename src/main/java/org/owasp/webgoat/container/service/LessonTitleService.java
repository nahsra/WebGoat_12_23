package org.owasp.webgoat.container.service;

import org.owasp.webgoat.container.lessons.Lesson;
import org.owasp.webgoat.container.session.WebSession;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LessonTitleService class.
 *
 * @author dm
 * @version $Id: $Id
 */
@RestController
public class LessonTitleService {

  private final WebSession webSession;

  public LessonTitleService(final WebSession webSession) {
    this.webSession = webSession;
  }

  /**
   * Returns the title for the current attack
   *
   * @return a {@link java.lang.String} object.
   */
  @RequestMapping(path = "/service/lessontitle.mvc", produces = "application/html")  public String showPlan() {
    Lesson lesson = webSession.getCurrentLesson();
    return lesson != null ? lesson.getTitle() : "";
  }
}
