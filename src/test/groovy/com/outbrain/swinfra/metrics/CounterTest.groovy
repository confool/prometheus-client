package com.outbrain.swinfra.metrics

import com.outbrain.swinfra.metrics.samples.SampleCreator
import com.outbrain.swinfra.metrics.samples.StaticLablesSampleCreator
import spock.lang.Specification
import spock.lang.Unroll

import static com.outbrain.swinfra.metrics.Counter.CounterBuilder
import static io.prometheus.client.Collector.MetricFamilySamples
import static io.prometheus.client.Collector.MetricFamilySamples.Sample
import static io.prometheus.client.Collector.Type.COUNTER
import static java.util.Collections.emptyList

class CounterTest extends Specification {

    private static final String NAME = "NAME"
    private static final String HELP = "HELP"
    private static final SampleCreator sampleCreator = new StaticLablesSampleCreator(Collections.emptyMap())

    @Unroll
    def 'Counter should return #expectedValue after incrementing #increment times'() {
        given:
        final Counter counter = new CounterBuilder(NAME, HELP).build()

        when:
        if (increment > 0) {
            1.upto(increment, { counter.inc() })
        }

        then:
        counter.getValue() == expectedValue

        where:
        increment | expectedValue
        0         | 0
        1         | 1
        10        | 10
    }

    @Unroll
    def 'Counter should return #expectedValue after incrementing by #incrementBy 10 times'() {
        given:
            final Counter counter = new CounterBuilder(NAME, HELP).build()

        when:
            if (incrementBy > 0) {
                1.upto(10, { counter.inc(incrementBy) })
            }

        then:
            counter.getValue() == expectedValue

        where:
            incrementBy | expectedValue
            0           | 0
            1           | 10
            10          | 100
    }

    def 'Counter should return the correct samples with labels defined'() {
        final String[] labelNames = ["label1", "label2"]
        final String[] labelValues1 = ["val1", "val2"]
        final String[] labelValues2 = ["val3", "val4"]

        given:
        final Sample sample1 = new Sample(NAME, Arrays.asList(labelNames), Arrays.asList(labelValues1), 5)
        final Sample sample2 = new Sample(NAME, Arrays.asList(labelNames), Arrays.asList(labelValues2), 6)
        final List<MetricFamilySamples> metricFamilySamples = [
            new MetricFamilySamples(NAME, COUNTER, HELP, [sample1, sample2])]

        when:
        final Counter counter = new CounterBuilder(NAME, HELP)
            .withLabels("label1", "label2")
            .build()

        counter.inc(5, labelValues1)
        counter.inc(6, labelValues2)

        then:
        counter.getSamples(sampleCreator).sort() == metricFamilySamples.sort()
    }

    def 'Counter should return the correct samples with subsystem defined'() {
        final String subsystem = "myNamespace"
        final String fullName = subsystem + "_" + NAME

        given:
            final List<Sample> samples = [new Sample(fullName, emptyList(), emptyList(), 0)]
            final List<MetricFamilySamples> metricFamilySamples = [
            new MetricFamilySamples(fullName, COUNTER, HELP, samples)]

        when:
           final Counter counter = new CounterBuilder(NAME, HELP)
            .withSubsystem(subsystem)
            .build()

        then:
            counter.getSamples(sampleCreator).sort() == metricFamilySamples.sort()
    }

    def 'Counter should return the correct samples with namespace and subsystem defined'() {
        final String namespace = "myNamespace"
        final String subsystem = "mySubsystem"
        final String fullName = namespace + "_" + subsystem + "_" + NAME

        given:
            final List<Sample> samples = [new Sample(fullName, emptyList(), emptyList(), 0)]
         final List<MetricFamilySamples> metricFamilySamples = [
            new MetricFamilySamples(fullName, COUNTER, HELP, samples)]

        when:
             final Counter counter = new CounterBuilder(NAME, HELP)
                 .withNamespace(namespace)
                 .withSubsystem(subsystem)
                 .build()

        then:
          counter.getSamples(sampleCreator).sort() == metricFamilySamples.sort()
    }

    def 'Counter should return the correct samples with namespace defined'() {
        final String namespace = "mySubsystem"
        final String fullName = namespace + "_" + NAME

        given:
           final List<Sample> samples = [new Sample(fullName, emptyList(), emptyList(), 0)]
           final List<MetricFamilySamples> metricFamilySamples = [
            new MetricFamilySamples(fullName, COUNTER, HELP, samples)]

        when:
            final Counter counter = new CounterBuilder(NAME, HELP)
                .withNamespace(namespace)
                .build()

        then:
            counter.getSamples(sampleCreator).sort() == metricFamilySamples.sort()
    }
}
