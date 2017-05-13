package com.outbrain.swinfra.metrics

import com.outbrain.swinfra.metrics.data.MetricDataConsumer
import spock.lang.Specification
import spock.lang.Unroll

import java.util.function.Consumer

import static com.outbrain.swinfra.metrics.Summary.SummaryBuilder

class SummaryTest extends Specification {

    private static final String NAME = "mySummary"
    private static final String HELP = "HELP"

    private final Consumer<com.codahale.metrics.Histogram> consumer = Mock(Consumer)
    private final MetricDataConsumer metricDataConsumer = Mock(MetricDataConsumer)

    final TestClock clock = new TestClock()


    def 'consumeSummary is called in metricDataConsumer for each child'() {
        given:
            final Summary summary = new SummaryBuilder(NAME, HELP).withClock(clock).build()
            1.upto(1000, {
                clock.tick = it - 1
                summary.observe(it)
            })
        when:
            summary.forEachMetricData(metricDataConsumer)
        then:
            1 * metricDataConsumer.consumeSummary(summary, [],
                    {
                        it.count == 1000 &&
                                it.sum == (1..1000).sum() &&
                                it.median == 500 &&
                                it.get75thPercentile() == 750 &&
                                it.get95thPercentile() == 950 &&
                                it.get98thPercentile() == 980 &&
                                it.get99thPercentile() == 990 &&
                                it.get999thPercentile() == 999
                    })
            0 * metricDataConsumer._
    }

    def 'Summary with no labels should return correct samples for newly initialized metric'() {
        given:
            final Summary summary = new SummaryBuilder(NAME, HELP).withClock(clock).build()
        when:
            summary.forEachChild(consumer)
        then:
            1 * consumer.accept({ it.metric.count == 0 && it.labelValues == [] })
            0 * consumer.accept(_)
    }

    def 'Summary with no labels should return correct samples after some measurements'() {
        given:
            final Summary summary = new SummaryBuilder(NAME, HELP).withClock(clock).build()
            1.upto(1000, {
                clock.tick = it - 1
                summary.observe(it)
            })
        when:
            summary.forEachChild(consumer)
        then:
            1 * consumer.accept({ it.metric.count == 1000 && it.labelValues == [] })
            0 * consumer.accept(_)
    }

    def 'Summary with labels should return correct samples after some measurements'() {
        final List<String> labelNames = ['label1', 'label2']
        final List<String> labelValues1 = ['value1', 'value2']
        final List<String> labelValues2 = ['value3', 'value4']
        given:
            final Summary summary = new SummaryBuilder(NAME, HELP).withClock(clock)
                                                                  .
                    withReservoir().
                    withExponentiallyDecayingReservoir(
                            1028,
                            0.15) //Use a "custom" reservoir just for the sake of having compilation error if this is not working
                                                                  .
                    withLabels(labelNames as String[])
                                                                  .
                    build()
            1.upto(1000, {
                clock.tick = it - 1
                summary.observe(it, labelValues1 as String[])
                summary.observe(it, labelValues2 as String[])
            })

        when:
            summary.forEachMetricData(metricDataConsumer)
        then:
            1 * metricDataConsumer.consumeSummary(summary, labelValues1,
                    {
                        it.count == 1000 &&
                                it.sum == (1..1000).sum() &&
                                it.median == 500 &&
                                it.get75thPercentile() == 750 &&
                                it.get95thPercentile() == 950 &&
                                it.get98thPercentile() == 980 &&
                                it.get99thPercentile() == 990 &&
                                it.get999thPercentile() == 999
                    })
            1 * metricDataConsumer.consumeSummary(summary, labelValues2,
                    {
                        it.count == 1000 &&
                                it.sum == (1..1000).sum() &&
                                it.median == 500 &&
                                it.get75thPercentile() == 750 &&
                                it.get95thPercentile() == 950 &&
                                it.get98thPercentile() == 980 &&
                                it.get99thPercentile() == 990 &&
                                it.get999thPercentile() == 999
                    })
            0 * metricDataConsumer._
    }

    def 'Summary with labels should return correct children'() {
        final List<String> labelNames = ['label1', 'label2']
        final List<String> labelValues1 = ['value1', 'value2']
        final List<String> labelValues2 = ['value3', 'value4']
        given:
            final Summary summary = new SummaryBuilder(NAME, HELP).withClock(clock)
                    .
                    withReservoir().
                    withExponentiallyDecayingReservoir(
                            1028,
                            0.15) //Use a "custom" reservoir just for the sake of having compilation error if this is not working
                    .
                    withLabels(labelNames as String[])
                    .
                    build()
            1.upto(1000, {
                clock.tick = it - 1
                summary.observe(it, labelValues1 as String[])
                summary.observe(it, labelValues2 as String[])
            })

        when:
            summary.forEachChild(consumer)
        then:
            1 * consumer.accept({ it.metric.count == 1000 && it.labelValues == labelValues1 })
            1 * consumer.accept({ it.metric.count == 1000 && it.labelValues == labelValues2 })
            0 * consumer.accept(_)
    }

    def 'Timer should add one sample for each time it is started and then stopped'() {
        given:
            long startTime = System.currentTimeMillis()
            clock.tick = startTime
            final double sum = (1..1000).sum() as double
            final Summary summary = new SummaryBuilder(NAME, HELP).withClock(clock).
                    withReservoir().withExponentiallyDecayingReservoir(1024, 0.00001).build()


            //Make a 1000 measurements with values 1, 2, 3, ... 1000
            1.upto(1000, {
                clock.tick = startTime
                final com.outbrain.swinfra.metrics.timing.Timer timer = summary.startTimer()
                clock.tick = startTime + it
                timer.stop()
            })
        when:
            summary.forEachMetricData(metricDataConsumer)
        then:
            1 * metricDataConsumer.consumeSummary(summary, [],
                    {
                        it.count == 1000 &&
                                it.sum == (1..1000).sum() &&
                                it.median == 501 &&
                                it.get75thPercentile() == 751 &&
                                it.get95thPercentile() == 951 &&
                                it.get98thPercentile() == 981 &&
                                it.get99thPercentile() == 991 &&
                                it.get999thPercentile() == 1000
                    })
            0 * metricDataConsumer._
    }

    def 'Summary without labels should throw an exception when attempting to observe a value with labels'() {
        given:
            final Summary summary = new SummaryBuilder(NAME, HELP).build()

        when:
            summary.observe(1, "labelValue")

        then:
            thrown(IllegalArgumentException.class)
    }

    @Unroll
    def 'Summary with labels should throw an exception when attempting to observe a value with labels #labels'() {
        given:
            final Summary summary = new SummaryBuilder(NAME, HELP).withLabels("l1", "l2").build()

        when:
            summary.observe(1, labels as String[])

        then:
            thrown(IllegalArgumentException.class)

        where:
            labels << [[], ["v1", ""], ["v1", "v2", "v3"]]
    }

}
