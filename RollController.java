package otel;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.StatusCode;

@RestController
public class RollController {
  private static final Logger logger = LoggerFactory.getLogger(RollController.class);

  @GetMapping("/rolldice")
  public String index(@RequestParam("player") Optional<String> player) {
    Span currentSpan = Span.current(); //Initialize the current span
    // Adding custom attributes to the current span
    currentSpan.setAttribute("app.rollcontroller.invocation", true);
    currentSpan.setAttribute("app.rollcontroller.player", player.orElse("anonymous"));
    currentSpan.setAttribute("app.rollcontroller.timestamp", System.currentTimeMillis());
    currentSpan.setAttribute("app.rollcontroller.randomnumber.min", 1);
    currentSpan.setAttribute("app.rollcontroller.randomnumber.max", 6);

    int result = this.getRandomNumber(1, 6);
    // Added a custom span event
    currentSpan.addEvent("Dice Rolled", Attributes.of(
        AttributeKey.longKey("app.rollcontroller.randomnumber.result"), (long) result));

    if (player.isPresent()) {
      if(player.get().equals("lito")) {
        this.traceExceptionImplementation();
      } else {
        currentSpan.setStatus(StatusCode.OK, "Player rolled the dice successfully");
        currentSpan.addEvent("Player Specified", Attributes.of(
            AttributeKey.stringKey("app.rollcontroller.player.name"), player.get()));

        logger.info("{} is rolling the dice: {}", player.get(), result);
      }
      // Added event that the player name was specified

    } else {
      // Added event that the player name was not specified
      currentSpan.addEvent("Player Not Specified");
      logger.info("Anonymous player is rolling the dice: {}", result);
    }
    return Integer.toString(result);
  }

  public int getRandomNumber(int min, int max) {
    return ThreadLocalRandom.current().nextInt(min, max + 1);
  }

  public void traceExceptionImplementation() {
    try {
      throw new Exception("Player lito is banned from the game and can't roll the dice for some time.");
    } catch (Exception e) {
      Span currentSpan = Span.current();
      currentSpan.recordException(e);
      currentSpan.setStatus(StatusCode.ERROR, "Exception occurred");
      logger.error("Exception traced in span", e);
    }
  }
}
