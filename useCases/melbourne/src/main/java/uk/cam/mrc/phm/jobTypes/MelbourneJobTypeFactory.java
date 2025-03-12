package uk.cam.mrc.phm.jobTypes;

import de.tum.bgu.msm.data.jobTypes.JobType;
import de.tum.bgu.msm.data.jobTypes.JobTypeFactory;

public class MelbourneJobTypeFactory implements JobTypeFactory {
    @Override
    public JobType getType(String id) {
        return MelbourneJobType.valueOf(id.toUpperCase());
    }
}
