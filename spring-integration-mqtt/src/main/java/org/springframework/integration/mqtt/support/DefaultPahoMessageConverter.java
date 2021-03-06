/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.mqtt.support;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.converter.MessageConversionException;
import org.springframework.util.Assert;


/**
 * Default implementation allowing most connection options to be configured.
 * @author Gary Russell
 * @since 1.0
 *
 */
public class DefaultPahoMessageConverter implements MqttMessageConverter {

	private final String charset;

	private final Integer defaultQos;

	private final Boolean defaultRetained;

	public DefaultPahoMessageConverter() {
		this (0, false);
	}

	public DefaultPahoMessageConverter(int defaultQos, boolean defaultRetain) {
		this(defaultQos, defaultRetain, "UTF-8");
	}

	public DefaultPahoMessageConverter(int defaultQos, boolean defaultRetained, String charset) {
		this.defaultQos = defaultQos;
		this.defaultRetained = defaultRetained;
		this.charset = charset;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <P> Message<P> toMessage(Object object) {
		return (Message<P>) toMessage(null, object);
	}

	public Message<String> toMessage(String topic, Object object) {
		Assert.isInstanceOf(MqttMessage.class, object);
		MqttMessage message = (MqttMessage) object;
		try {
			MessageBuilder<String> messageBuilder = MessageBuilder.withPayload(new String(message.getPayload(), this.charset))
					.setHeader(MqttHeaders.QOS, message.getQos())
					.setHeader(MqttHeaders.DUPLICATE, message.isDuplicate())
					.setHeader(MqttHeaders.RETAINED, message.isRetained());
			if (topic != null) {
				messageBuilder.setHeader(MqttHeaders.TOPIC, topic);
			}
			return messageBuilder.build();
		}
		catch (Exception e) {
			throw new MessageConversionException("failed to convert object to Message", e);
		}
	}

	@Override
	public <P> Object fromMessage(Message<P> message) {
		Object payload = message.getPayload();
		Assert.isTrue(payload instanceof byte[] || payload instanceof String);
		byte[] payloadBytes;
		if (payload instanceof String) {
			try {
				payloadBytes = ((String) payload).getBytes(this.charset);
			}
			catch (Exception e) {
				throw new MessageConversionException("failed to convert Message to object", e);
			}
		}
		else {
			payloadBytes = (byte[]) payload;
		}
		MqttMessage mqttMessage = new MqttMessage(payloadBytes);
		Object header = message.getHeaders().get(MqttHeaders.RETAINED);
		Assert.isTrue(header == null || header instanceof Boolean, MqttHeaders.RETAINED + " header must be Boolean");
		mqttMessage.setRetained(header == null ? this.defaultRetained : (Boolean) header);
		header = message.getHeaders().get(MqttHeaders.QOS);
		Assert.isTrue(header == null || header instanceof Integer, MqttHeaders.QOS + " header must be Integer");
		mqttMessage.setQos(header == null ? this.defaultQos : (Integer) header);
		return mqttMessage;
	}

}
