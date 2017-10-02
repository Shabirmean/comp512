FROM ubuntu
RUN apt-get update
RUN apt-get install -y git \
	default-jre \
	default-jdk \
	wget
RUN git clone https://github.com/PorscheFanboy/512
RUN cd /512/servercode && export CLASSPATH=/512/servercode && javac ResInterface/ResourceManager.java && jar cvf ResInterface.jar ResInterface/*.class && javac ResImpl/ResourceManagerImpl.java && cp ResInterface.jar ../clientsrc/
RUN cd 512/clientsrc && export CLASSPATH=/512/clientsrc:/512/clientsrc/ResInterface.jar && javac client.java


