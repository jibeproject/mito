package uk.cam.mrc.phm.util;

import de.tum.bgu.msm.data.jobTypes.JobTypeFactory;
import de.tum.bgu.msm.util.ImplementationConfig;
import uk.cam.mrc.phm.jobTypes.MelbourneJobTypeFactory;

public class MelbourneImplementationConfig implements ImplementationConfig {

    private final static MelbourneImplementationConfig instance = new MelbourneImplementationConfig();

    private MelbourneImplementationConfig() {}

    public static MelbourneImplementationConfig get() {
        return instance;
    }

    @Override
    public JobTypeFactory getJobTypeFactory() {
        return new MelbourneJobTypeFactory();
    }
}
