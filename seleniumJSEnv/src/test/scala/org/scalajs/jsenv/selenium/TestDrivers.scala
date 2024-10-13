package org.scalajs.jsenv.selenium

import org.openqa.selenium.Capabilities
import org.openqa.selenium.firefox.{FirefoxOptions, FirefoxDriverLogLevel}
import org.openqa.selenium.chrome.ChromeOptions

import java.util.logging.{Logger, Level}
import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.scalajs.jsenv.selenium.SeleniumJSEnv.DriverFactory

object TestDrivers {
  // Lower the logging level for Selenium to avoid spam.
  Logger.getLogger("org.openqa.selenium").setLevel(Level.WARNING)

  val fromEnv: DriverFactory = () => nameFromEnv match {
    case "firefox" =>
      new FirefoxDriver(
        new FirefoxOptions()
          .addArguments("--headless")
          .setLogLevel(FirefoxDriverLogLevel.ERROR)
      )

    case "chrome" =>
      new ChromeDriver(
        new ChromeOptions()
          .addArguments("--headless")
      )

    case name =>
      throw new IllegalArgumentException(s"Unknown browser $name")
  }

  def nameFromEnv: String = sys.env.getOrElse("SJS_TEST_BROWSER", "firefox")
}
