FROM ubuntu
RUN apt-get update
RUN apt-get install -y git \
	default-jre \
	default-jdk
RUN git clone https://github.com/PorscheFanboy/512
RUN export CLASSPATH=/512/servercode
RUN cd /512/servercode && javac ResInterface/ResourceManager.java && jar cvf ResInterface.jar ResInterface/*.class && javac ResImpl/ResourceManagerImpl.java



