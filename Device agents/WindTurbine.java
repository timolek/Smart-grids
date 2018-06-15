package net.powermatcher.examples;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
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
import net.powermatcher.api.messages.PriceUpdate;
import net.powermatcher.api.monitoring.ObservableAgent;
import net.powermatcher.core.BaseAgentEndpoint;

/**
 * Wind Turbine agent.
 *
 * @author FAN
 * @version 2.0
 */

@Component(designateFactory = WindTurbine.Config.class,
           immediate = true,
           provide = { ObservableAgent.class, AgentEndpoint.class })
public class WindTurbine
    extends BaseAgentEndpoint
    implements AgentEndpoint {

    private static final String DATA_FOLDER = "/Users/Timo-PC/dropbox/Honours project/Windpower.txt"; // location as to be changed to the location of the txt-file

    private static final Logger LOGGER = LoggerFactory.getLogger(WindTurbine.class);

    private Config config;

    public static interface Config {
        @Meta.AD(deflt = "WindTurbine", description = "The uniue identifier of the agent")
        String agentId();

        @Meta.AD(deflt = "concentrator",
                 description = "The agent identifier of the parent matcher to which this agent should be connected")
        public String desiredParentId();

        @Meta.AD(deflt = "5", description = "Number of seconds between bid updates")
        long bidUpdateRate();

    }

    /**
     * A delayed result-bearing action that can be cancelled.
     */
    private ScheduledFuture<?> scheduledFuture;
    private double[] dataPower;

    /**
     * OSGi calls this method to activate a managed service.
     *
     * @param properties
     *            the configuration properties
     */
    @Activate
    public void activate(Map<String, Object> properties) {
        config = Configurable.createConfigurable(Config.class, properties);
        init(config.agentId(), config.desiredParentId());
        LOGGER.info("Agent [{}], activated", config.agentId());
        dataPower = readData();
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
     * In this function the demand pattern of the agent is loaded.
     */
    double[] readData() {

        try {
            File windData = new File(DATA_FOLDER);
            BufferedReader reader = new BufferedReader(new FileReader(windData));
            String line;
            double[] data = new double[365 * 24];
            for (int i = 0; (line = reader.readLine()) != null; i++) {
                data[i] = Double.parseDouble(line);
            }
            return data;
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    /*
     * This method is to get the demand that corresponds to the current time.
     */
    double getPower() {
        Date timeStamp = context.currentTime();
        long miliseconds = timeStamp.getTime();

        Calendar oldCal = Calendar.getInstance();
        oldCal.clear();
        oldCal.set(2018, Calendar.JANUARY, 1, 00, 00, 00);
        Date oldDate = oldCal.getTime();
        long oldDateMili = oldDate.getTime();

        long minusYear = miliseconds - oldDateMili;
        long hours = minusYear / (1000L * 60L * 60L);
        int numberHours = (int) hours;
        return dataPower[numberHours] * -1;

    }

    /**
     * Here a bid is generated and placed.
     */
    void doBidUpdate() {
        AgentEndpoint.Status currentStatus = getStatus();
        if (currentStatus.isConnected()) {
            double demand = getPower();
            publishBid(Bid.flatDemand(currentStatus.getMarketBasis(), demand));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void handlePriceUpdate(PriceUpdate priceUpdate) {
        super.handlePriceUpdate(priceUpdate);
        // Nothing to control for a WindTurbine
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public void setContext(FlexiblePowerContext context) {
        super.setContext(context);
        scheduledFuture = context.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                doBidUpdate();
            }
        }, Measure.valueOf(0, SI.SECOND), Measure.valueOf(config.bidUpdateRate(), SI.SECOND));
    }
}
