package net.frontlinesms.camel.smslib;

import java.util.LinkedList;
import java.util.Map;

import org.smslib.CIncomingMessage;
import org.smslib.CService;
import org.smslib.CService.MessageClass;

public class SmslibService {
	private final CService cService;
	
	private SmslibProducer producer;
	private SmslibConsumer consumer;
	
	private boolean consumerRunning;
	private boolean producerRunning;

	public SmslibService(CServiceFactory cServiceFactory, String uri, String remaining, Map<String, Object> parameters) {
		this.cService = cServiceFactory.create(uri, remaining, parameters);
	}
	
	public synchronized void startForConsumer() throws Exception {
		assert(consumer != null);
		consumerRunning = true;
		new ReceiveThread().start();
		startService();
	}

	public synchronized void startForProducer() throws Exception {
		assert(producer != null);
		producerRunning = true;
		startService();
	}
	
	public synchronized void stopForProducer() throws Exception {
		assert(producer != null);
		producerRunning = false;
		stopIfUnused();
	}
	
	public synchronized void stopForConsumer() throws Exception {
		assert(consumer != null);
		consumerRunning = false;
		stopIfUnused();
	}

	private synchronized void stopIfUnused() throws Exception {
		if(cService.isConnected()
				&& !producerRunning
				&& !consumerRunning) {
			cService.disconnect();
		}
	}

	private synchronized void startService() throws Exception {
		if(!cService.isConnected()) {
			cService.connect();
		}
	}

	public void send(OutgoingSmslibCamelMessage message) throws Exception {
		this.cService.sendMessage(message.getCMessage());
	}

	public void doReceive() throws Exception {
		LinkedList<CIncomingMessage> messageList = new LinkedList<CIncomingMessage>();
		this.cService.readMessages(messageList, MessageClass.UNREAD);
		for(CIncomingMessage m : messageList) {
			this.consumer.accept(new IncomingSmslibCamelMessage(m));
		}
	}

	public synchronized SmslibProducer getProducer() {
		return producer;
	}
	public synchronized void setProducer(SmslibProducer producer) throws SmslibServiceException {
		if(this.producer != null) {
			throw new SmslibServiceException("Producer already set.");
		}
		this.producer = producer;
	}

	public synchronized SmslibConsumer getConsumer() {
		return consumer;
	}
	public synchronized void setConsumer(SmslibConsumer consumer) throws SmslibServiceException {
		if(this.consumer != null) {
			throw new SmslibServiceException("Consumer already set.");
		}
		this.consumer = consumer;
	}
	
	class ReceiveThread extends Thread {
		public void run() {
			while(consumerRunning) {
				try {
					doReceive();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
