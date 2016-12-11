package com.outbrain.swinfra.metrics;

import com.codahale.metrics.Metric;
import com.outbrain.swinfra.metrics.children.ChildMetricRepo;
import com.outbrain.swinfra.metrics.children.MetricData;
import com.outbrain.swinfra.metrics.samples.SampleCreator;
import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

abstract class AbstractMetric<T extends Metric> {

  private final String name;
  private final String help;
  private final List<String> labelNames;
  private ChildMetricRepo<T> childMetricRepo;

  AbstractMetric(final String name,
                 final String help,
                 final String[] labelNames) {
    this.name = name;
    this.help = help;
    this.labelNames = Arrays.asList(labelNames);
  }

  abstract ChildMetricRepo<T> createChildMetricRepo();

  abstract Collector.Type getType();

  abstract List<Sample> createSamples(MetricData<T> metricData, SampleCreator sampleCreator);

  String getName() {
    return name;
  }

  List<String> getLabelNames() {
    return labelNames;
  }

  private MetricFamilySamples toMetricFamilySamples(final MetricData<T> metricData, final SampleCreator sampleCreator) {
    return new MetricFamilySamples(name, getType(), help, createSamples(metricData, sampleCreator));
  }

  void initChildMetricRepo() {
    this.childMetricRepo = createChildMetricRepo();
  }

  void validateLabelValues(final String... labelValues) {
    if (labelNames.size() > 0) {
      Validate.isTrue(labelNames.size() == labelValues.length, "A label value must be supplied for each label name");
    }

    for (final String labelName : labelNames) {
      Validate.notBlank(labelName, "Label names must contain text");
    }
  }

  T metricForLabels(final String... labelValues) {
    return childMetricRepo.metricForLabels(labelValues).getMetric();
  }

  List<MetricFamilySamples> getSamples(final SampleCreator sampleCreator) {
    //todo make this return a single MetricFamilySamples object
    final List<Sample> samples = childMetricRepo
        .all().stream()
        .flatMap(metricData -> createSamples(metricData, sampleCreator).stream())
        .collect(Collectors.toList());
    return Collections.singletonList(new MetricFamilySamples(name, getType(), help, samples));
  }

}
