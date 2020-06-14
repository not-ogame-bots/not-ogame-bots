package not.ogame.bots.selenium

trait GecoDriver { outer =>
  System.setProperty("webdriver.gecko.driver", outer.getClass.getResource("geckodriver").getFile)
}
