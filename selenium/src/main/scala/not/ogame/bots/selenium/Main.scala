package not.ogame.bots.selenium

import not.ogame.bots.Credentials

object Main {

  def main(args: Array[String]): Unit = {
    System.setProperty("webdriver.gecko.driver", "selenium/geckodriver")
    val ogameDriver = new SeleniumOgameDriverCreator().create(Credentials("fire@fire.pl", "1qaz2wsx", "Mensa", "s165-pl"))
    ogameDriver.login().unsafeRunSync()
  }
}
