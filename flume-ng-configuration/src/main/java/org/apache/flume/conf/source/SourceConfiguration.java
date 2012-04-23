/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.flume.conf.source;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.flume.Context;
import org.apache.flume.conf.ComponentConfiguration;
import org.apache.flume.conf.ComponentConfigurationFactory;
import org.apache.flume.conf.ConfigurationException;
import org.apache.flume.conf.FlumeConfiguration;
import org.apache.flume.conf.FlumeConfigurationError;
import org.apache.flume.conf.FlumeConfigurationErrorType;
import org.apache.flume.conf.FlumeConfigurationError.ErrorOrWarning;
import org.apache.flume.conf.channel.ChannelSelectorConfiguration;
import org.apache.flume.conf.channel.ChannelSelectorConfiguration.ChannelSelectorConfigurationType;
import org.apache.flume.conf.channel.ChannelSelectorType;
import org.apache.flume.conf.channel.ChannelConfiguration.ChannelConfigurationType;

public class SourceConfiguration extends ComponentConfiguration {
  protected Set<String> channels;
  protected ChannelSelectorConfiguration selectorConf;
  private static final String CHANNELS_CONF = "channels";
  public SourceConfiguration(String componentName) {
    super(componentName);
  }

  public Set<String> getChannels() {
    return channels;
  }

  public ChannelSelectorConfiguration getSelectorConfiguration() {
    return selectorConf;
  }

  public void configure(Context context) throws ConfigurationException {
    super.configure(context);
    try {
      String channelList = context.getString(CHANNELS_CONF);
      if (channelList != null) {
        this.channels =
            new HashSet<String>(Arrays.asList(channelList.split("\\s+")));
        if (channels.isEmpty()) {
          errors.add(new FlumeConfigurationError(componentName,
              ComponentType.CHANNEL.getComponentType(),
              FlumeConfigurationErrorType.PROPERTY_VALUE_NULL,
              ErrorOrWarning.ERROR));
          throw new ConfigurationException("No channels set for "
              + this.getComponentName());
        }
      }
      Map<String, String> selectorParams =
          context.getSubProperties("selector.");
      String selType;
      if (selectorParams != null && !selectorParams.isEmpty()) {
        selType = selectorParams.get(FlumeConfiguration.CONF_TYPE);
        System.out.println("Loading selector: " + selType);
      } else {
        selType = ChannelSelectorConfigurationType.REPLICATING.toString();
      }

      if (selType == null || selType.isEmpty()) {
        selType = ChannelSelectorConfigurationType.REPLICATING.toString();

      }
      ChannelSelectorType selectorType =
          this.getKnownChannelSelector(selType);
      Context selectorContext = new Context();
      selectorContext.putAll(selectorParams);
      String config = null;
      if (selectorType == null) {
        config = selectorContext.getString(FlumeConfiguration.CONF_CONFIG);
        if (config == null || config.isEmpty()) {
          config = "OTHER";
        }
      } else {
        config = selectorType.toString().toUpperCase();
      }

      this.selectorConf =
          (ChannelSelectorConfiguration) ComponentConfigurationFactory
              .create(ComponentType.CHANNELSELECTOR.getComponentType(), config,
                  ComponentType.CHANNELSELECTOR);
      selectorConf.setChannels(channels);
      selectorConf.configure(selectorContext);
    } catch (Exception e) {
      errors.add(new FlumeConfigurationError(componentName,
          ComponentType.CHANNELSELECTOR.getComponentType(),
          FlumeConfigurationErrorType.CONFIG_ERROR,
          ErrorOrWarning.ERROR));
      throw new ConfigurationException("Failed to configure component!", e);
    }
  }

  private ChannelSelectorType getKnownChannelSelector(String type) {
    ChannelSelectorType[] values = ChannelSelectorType.values();
    for (ChannelSelectorType value : values) {
      if (value.toString().equalsIgnoreCase(type)) return value;
      String clName = value.getChannelSelectorClassName();
      if (clName != null && clName.equalsIgnoreCase(type)) return value;
    }
    return null;
  }

  public enum SourceConfigurationType {
    OTHER(null),

    SEQ(null),
    /**
     * Netcat source.
     *
     * @see NetcatSource
     */
    NETCAT("org.apache.flume.conf.source.NetcatSourceConfiguration"),

    /**
     * Exec source.
     *
     * @see ExecSource
     */
    EXEC("org.apache.flume.conf.source.ExecSourceConfiguration"),

    /**
     * Avro soruce.
     *
     * @see AvroSource
     */
    AVRO("org.apache.flume.conf.source.AvroSourceConfiguration"),

    /**
     * Syslog Tcp Source
     *
     * @see org.apache.flume.source.SyslogTcpSource
     */
    SYSLOGTCP("org.apache.flume.conf.source.SyslogTcpSourceConfiguration"),

    /**
     * Syslog Udp Source
     *
     * @see org.apache.flume.source.SyslogUDPSource
     */

    SYSLOGUDP("org.apache.flume.conf.source.SyslogUDPSourceConfiguration");

    private String srcConfigurationName;

    private SourceConfigurationType(String src) {
      this.srcConfigurationName = src;
    }

    public String getSourceConfigurationType() {
      return this.getSourceConfigurationType();
    }

    @SuppressWarnings("unchecked")
    public SourceConfiguration getConfiguration(String name)
        throws ConfigurationException {
      if (this.equals(ChannelConfigurationType.OTHER)) {
        return new SourceConfiguration(name);
      }
      Class<? extends SourceConfiguration> clazz = null;
      SourceConfiguration instance = null;
      try {
        if (srcConfigurationName != null) {
          clazz =
              (Class<? extends SourceConfiguration>) Class
                  .forName(srcConfigurationName);
          instance = clazz.getConstructor(String.class).newInstance(name);
        }
        else {
          return new SourceConfiguration(name);
        }
      } catch (ClassNotFoundException e) {
        // Could not find the configuration stub, do basic validation
        instance = new SourceConfiguration(name);
        // Let the caller know that this was created because of this exception.
        instance.setNotFoundConfigClass();
      } catch (Exception e) {
        throw new ConfigurationException("Error creating configuration", e);
      }
      return instance;
    }
  }

}