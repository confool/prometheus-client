package com.outbrain.swinfra.metrics;

import org.apache.commons.lang3.Validate;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public abstract class AbstractMetricBuilder<T extends AbstractMetric, B extends AbstractMetricBuilder<T, B>> {

  private final String name;
  private final String help;

  private String namespace = "";
  private String subsystem = "";
  String[] labelNames = new String[] {};

  AbstractMetricBuilder(final String name, final String help) {
    this.name = name;
    this.help = help;
  }

  public B withSubsystem(final String subsystem) {
    this.subsystem = subsystem;
    return getThis();
  }

  public B withNamespace(final String namespace) {
    this.namespace = namespace;
    return getThis();
  }

  public B withLabels(final String... labelNames) {
    this.labelNames = labelNames;
    return getThis();
  }

  protected abstract T create(final String fullName, final String help, final String[] labelNames);

  public T build() {
    validateParams();
    final T metric = create(createFullName(), help, labelNames);
    metric.initChildMetricRepo();
    return metric;
  }

  void validateParams() {
    Validate.notBlank(name, "The metric's name must contain text");
    Validate.notBlank(help, "The metric's help must contain text");
    for (final String labelName : labelNames) {
      Validate.notBlank(labelName, "Label names must contain text");
    }
  }

  private String createFullName() {
    final StringBuilder sb = new StringBuilder(name);
    if (isNotBlank(subsystem)) sb.insert(0, subsystem + "_");
    if (isNotBlank(namespace)) sb.insert(0, namespace + "_");
    return sb.toString();
  }

  @SuppressWarnings("unchecked")
  private B getThis() {
    return (B) this;
  }
}
