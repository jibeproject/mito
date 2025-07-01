package uk.cam.mrc.phm.jobTypes;

import de.tum.bgu.msm.data.jobTypes.Category;
import de.tum.bgu.msm.data.jobTypes.JobType;

public enum MelbourneJobType implements JobType{
    TOT (Category.OTHER);

    private final Category category;

    MelbourneJobType(Category category) {
        this.category = category;
    }

    @Override
    public Category getCategory() {
        return category;
    }
}
