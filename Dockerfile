FROM selenium/standalone-firefox
# Install sbt
WORKDIR /tmp
RUN \
  curl -L -o sbt-1.2.8.deb https://dl.bintray.com/sbt/debian/sbt-1.2.8.deb && \
  sudo dpkg -i sbt-1.2.8.deb && \
  rm sbt-1.2.8.deb && \
  sudo apt-get update && \
  sudo apt-get install sbt
