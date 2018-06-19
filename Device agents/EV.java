package net.powermatcher.examples;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.flexiblepower.context.FlexiblePowerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;
import net.powermatcher.api.AgentEndpoint;
import net.powermatcher.api.data.Bid;
import net.powermatcher.api.data.PointBidBuilder;
import net.powermatcher.api.messages.PriceUpdate;
import net.powermatcher.api.monitoring.ObservableAgent;
import net.powermatcher.core.BaseAgentEndpoint;

/**
 * THE EV agent.
 */
@Component(designateFactory = EV.Config.class,
           immediate = true,
           provide = { ObservableAgent.class, AgentEndpoint.class })
public class EV
    extends BaseAgentEndpoint
    implements AgentEndpoint {

    private static final String OUTPUT_FOLDER = "/Users/Timo-PC/Desktop/oktober10/"; // Has to be changed to the desired
                                                                                     // output folder

    private static final Logger LOGGER = LoggerFactory.getLogger(EV.class);

    public static interface Config {
        @Meta.AD(deflt = "EV", description = "The unique identifier of the agent")
        String agentId();

        @Meta.AD(deflt = "concentrator",
                 description = "The agent identifier of the parent matcher to which this agent should be connected")
        public String desiredParentId();

        @Meta.AD(deflt = "10", description = "Number of seconds between bid updates")
        long bidUpdateRate();
    }

    /**
     * A delayed result-bearing action that can be cancelled.
     */
    private ScheduledFuture<?> scheduledFuture;

    private double batteryCapacity;

    private double stateOfCharge;

    private double loadingspeed;

    private Bid previousBid;

    private long lastUpdate;

    private double deadline;

    private Config config;

    private double distance;

    private double timeOffline;

    /**
     * OSGi calls this method to activate a managed service.
     *
     * @param properties
     *            the configuration properties
     */
    @Activate
    public void activate(Map<String, Object> properties) {
        try {
            config = Configurable.createConfigurable(Config.class, properties);
            init(config.agentId(), config.desiredParentId());
            LOGGER.info("Agent [{}], activated", config.agentId());
            loadingspeed = 3000; // Has to multiplied by 1.25, 2.5, 3.75 or 5.0 for the cases of 25%, 50%, 75%, and
                                 // 100%.
            batteryCapacity = 100000; // Has to multiplied by 1.25, 2.5, 3.75 or 5.0 for the cases of 25%, 50%, 75%, and
                                      // 100%.
            previousBid = null;
        } catch (Throwable e) {
            LOGGER.error("error activation", e);
        }
    }

    /**
     * OSGi calls this method to deactivate a managed service.
     */
    @Override
    @Deactivate
    public void deactivate() {
        super.deactivate();
        scheduledFuture.cancel(false);
        LOGGER.info("Agent [{}], deactivated", getAgentId());
    }

    /*
     * This method is to initialize a new output file
     */
    public void openFile()
                           throws IOException {
        String agentId = getAgentId();
        String fileName = OUTPUT_FOLDER + agentId + ".txt";
        String str = "Demand";
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        writer.write(str);
        writer.close();
    }

    /*
     * This method is to write the demand to the generated output file
     */
    public void writeToFile(double demand)
                                           throws IOException {
        String agentId = getAgentId();
        String fileName = OUTPUT_FOLDER + agentId + ".txt";
        String currentDemand = String.valueOf(demand);
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));
        writer.append('\n');
        writer.append(currentDemand);
        writer.close();
    }

    /*
     * This method is to generate a deadline in which the vehicle needs to be charged. The distance that the car travels
     * is also randomly generated here. Furthermore, the deadline is only generated during daytime.
     */
    void generateDeadline() {

        Random generator = new Random();
        distance = generator.nextInt(100) + 1; // Has to multiplied by 1.25, 2.5, 3.75 or 5.0 for the cases of 25%, 50%,
                                               // 75%, and 100%.
        while ((stateOfCharge - ((distance / 5.7) * 1000) <= 0)) {
            distance = generator.nextInt(50) + 1;
        }
        timeOffline = distance / 50; // 50 as to multiplied by 1.25, 2.5, 3.75 or 5.0 for the cases of 25%, 50%, 75%,
                                     // and 100%.

        int hourOfDay = context.currentTime().getHours();
        if (((0 <= hourOfDay) && (hourOfDay <= 6)) || ((22 >= hourOfDay) && (hourOfDay >= 23))) {
            deadline = generator.nextInt(8) + 8;
        } else {
            deadline = generator.nextInt(16) + 1;
        }
    }

    /*
     * This method calculates the priority in which the vehicle has to charge.
     */
    double calculatePriority() {
        double timeNeeded = (batteryCapacity - stateOfCharge) / loadingspeed;
        double priority = timeNeeded / deadline;
        return priority;
    }

    /*
     * This method calculates the price for the bid of the agent.
     */
    private double priceOf(double priceFraction) {
        AgentEndpoint.Status currentStatus = getStatus();

        return currentStatus.getMarketBasis().getMinimumPrice() + priceFraction
                                                                  * (currentStatus.getMarketBasis().getMaximumPrice()
                                                                     - currentStatus.getMarketBasis()
                                                                                    .getMinimumPrice());
    }

    /**
     * Here a bid in generated and published.
     *
     *
     */

    void doBidUpdate() throws IOException {
        // LOGGER.info("BIDupdate")
        AgentEndpoint.Status currentStatus = getStatus();
        if (!currentStatus.isConnected()) {
            LOGGER.info("Not connected, not doing a bid update");
            return;
        }
        if (previousBid == null) {
            lastUpdate = context.currentTime().getTime();
            Random generator = new Random();
            double state = 100000; // Has to multiplied by 1.25, 2.5, 3.75 or 5.0 for the cases of 25%, 50%, 75%, and
                                   // 100%.
            stateOfCharge = generator.nextInt((int) state) + 1;
            generateDeadline();
            openFile();
        }
        Bid rawBid;
        double maxDemand = loadingspeed;
        double minDemand = -loadingspeed;
        double priority = calculatePriority();

        if (priority >= 1.0) {
            rawBid = Bid.flatDemand(currentStatus.getMarketBasis(), maxDemand);

        } else if (deadline < 0) {
            rawBid = Bid.flatDemand(currentStatus.getMarketBasis(), 0);
            if (timeOffline <= 0) {
                stateOfCharge -= ((distance / 5.7) * 1000);
                generateDeadline();
            } else {
                timeOffline -= 1;
            }

        } else if (stateOfCharge < 20000) { // 20000 Has to multiplied by 1.25, 2.5, 3.75 or 5.0 for the cases of 25%,
                                            // 50%, 75%, and 100%.
            rawBid = Bid.flatDemand(currentStatus.getMarketBasis(), maxDemand);
        } else if (stateOfCharge == batteryCapacity) {
            double minPriceFraction, maxPriceFraction;
            minPriceFraction = priority;
            maxPriceFraction = 1.0;
            double maxPrice = priceOf(maxPriceFraction);
            double minPrice = priceOf(minPriceFraction);
            if (Double.isNaN(maxPrice)) {
                maxPrice = currentStatus.getMarketBasis().getMaximumPrice();
            }
            if (Double.isNaN(minPrice)) {
                minPrice = currentStatus.getMarketBasis().getMinimumPrice();
            }
            rawBid = new PointBidBuilder(currentStatus.getMarketBasis()).add(maxPrice, minDemand)
                                                                        .add(minPrice, 0)
                                                                        .build();
        } else {
            double minPriceFraction, maxPriceFraction;
            minPriceFraction = priority;
            maxPriceFraction = 1.0;
            double maxPrice = priceOf(maxPriceFraction);
            double minPrice = priceOf(minPriceFraction);
            if (Double.isNaN(maxPrice)) {
                maxPrice = currentStatus.getMarketBasis().getMaximumPrice();
            }
            if (Double.isNaN(minPrice)) {
                minPrice = currentStatus.getMarketBasis().getMinimumPrice();
            }
            rawBid = new PointBidBuilder(currentStatus.getMarketBasis()).add(maxPrice, minDemand)
                                                                        .add(minPrice, maxDemand)
                                                                        .build();
        }
        previousBid = rawBid;
        publishBid(rawBid);
    }

    /**
     * Price updates in with which the state of the vehicle changes are handled here.
     */
    @Override
    public void handlePriceUpdate(PriceUpdate priceUpdate) {
        super.handlePriceUpdate(priceUpdate);
        if (previousBid != null) {

            double previousDemand = previousBid.getDemandAt(priceUpdate.getPrice());
            double timePast = context.currentTime().getTime() - lastUpdate;
            double hoursPast = timePast / 1000L / 3600L;
            try {
                writeToFile(previousDemand);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if ((stateOfCharge + (previousDemand * hoursPast)) < batteryCapacity) {
                stateOfCharge += (previousDemand * hoursPast);
            } else {
                stateOfCharge = batteryCapacity;
            }
            lastUpdate = context.currentTime().getTime();
            deadline -= hoursPast;
        }
    }

    @Override
    public void setContext(FlexiblePowerContext context) {
        super.setContext(context);
        scheduledFuture = context.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    doBidUpdate();
                } catch (Throwable e) {
                    LOGGER.error("error no bid update", e);
                }

            }
        }, Measure.valueOf(0, SI.SECOND), Measure.valueOf(config.bidUpdateRate(), SI.SECOND));
    }
}
