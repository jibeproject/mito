package routing;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public class ActiveConfigGroup extends ReflectiveConfigGroup {

    private List<ToDoubleFunction<Link>> attributes;
    private Function<Person,double[]> weights;

    public ActiveConfigGroup(String name) {
        super(name);
    }


    public void setAttributes(List<ToDoubleFunction<Link>> attributes) {
        this.attributes = attributes;
    }

    public void setWeights(Function<Person,double[]> weights) {
        this.weights = weights;
    }

    public List<ToDoubleFunction<Link>> getAttributes() { return attributes; }

    public Function<Person,double[]> getWeights() { return weights; }

    public String getMode() {
        return null;
    }


}
