package uk.cam.mrc.phm.io;

import de.tum.bgu.msm.data.*;
        import de.tum.bgu.msm.io.input.AbstractCsvReader;
import de.tum.bgu.msm.resources.Resources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.cam.mrc.phm.util.parseMEL;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;


public class PersonsReader7daysMEL extends AbstractCsvReader {

    private static final Logger logger = LogManager.getLogger(PersonsReader7daysMEL.class);

    private int posId = -1;
    private int posHhId = -1;
    private int posAge = -1;
    private int posSex = -1;
    private int posOccupation = -1;
    private int posWorkplaceId = -1;
    private int posLicence = -1;
    private int posIncome = -1;
    private int posSchoolId = -1;

    private int occupationCounter = 0;

    public PersonsReader7daysMEL(DataSet dataSet) {
        super(dataSet);
    }

    @Override
    public void read() {
        Path filePath = Resources.instance.getPersonsFilePath();
        logger.info("  Reading person micro data from ascii file ({})", filePath);
        super.read(filePath, ",");
        int noIncomeHouseholds = 0;
        for(MitoHousehold household: dataSet.getHouseholds().values()) {
            if(household.getMonthlyIncome() == 0) {
                noIncomeHouseholds++;
            }
        }
        if(noIncomeHouseholds > 0) {
            logger.warn("There are " + noIncomeHouseholds + " households with no income after reading all persons.");
        }
        logger.info("There are " + occupationCounter + " persons without occupation (student or worker).");
    }

    @Override
    public void processHeader(String[] header) {
        header = Arrays.stream(header).map(
                h -> h.replace("\"", "").trim()
        ).toArray(String[]::new);
        List<String> headerList = Arrays.asList(header);
        posId = headerList.indexOf("id");
        posHhId = headerList.indexOf("hhid");
        posAge = headerList.indexOf("age");
        posSex = headerList.indexOf("gender");
        posOccupation = headerList.indexOf("occupation");
        posWorkplaceId = headerList.indexOf("workplace");
        posSchoolId = headerList.indexOf("schoolId");
        posLicence = headerList.indexOf("driversLicence");
        posIncome = headerList.indexOf("income");
    }

    @Override
    public void processRecord(String[] record) {
        final int id = parseMEL.intParse(record[posId]);
        final int hhid = parseMEL.intParse(record[posHhId]);

        if(!dataSet.getHouseholds().containsKey(hhid)) {
            logger.warn("Person " + id + " refers to non-existing household " + hhid + ". Ignoring this person.");
            //return null;
        }
        MitoHousehold hh = dataSet.getHouseholds().get(hhid);

        final int age = Integer.parseInt(record[posAge]);
        final String genderString = record[posSex].replace("\"", "").trim();
        int genderCode;
        if ("Female".equalsIgnoreCase(genderString)) {
            genderCode = 2;
        } else if ("Male".equalsIgnoreCase(genderString)) {
            genderCode = 1;
        } else {
            throw new IllegalArgumentException("Invalid gender value: " + genderString);
        }
        MitoGender mitoGender = MitoGender.valueOf(genderCode);

        final int occupationCode = Integer.parseInt(record[posOccupation]);
        MitoOccupationStatus mitoOccupationStatus = MitoOccupationStatus.valueOf(occupationCode);

        final int workplace = parseMEL.intParse(record[posWorkplaceId]);

        final String schoolString = record[posSchoolId].replace("\"", "").trim();
        final int school;
        if ("NA".equalsIgnoreCase(schoolString)) {
            school = -1; // Use -1 to indicate missing data
        } else {
            school = Integer.parseInt(schoolString);
        }

        final boolean driversLicense = Boolean.parseBoolean(record[posLicence]);


        //mito uses monthly income, while SILO uses annual income

        int monthlyIncome = Integer.parseInt(record[posIncome])/12;
        hh.addIncome(monthlyIncome);

        MitoOccupation occupation = null;

        switch (mitoOccupationStatus) {
            case WORKER:
                if(dataSet.getJobs().containsKey(workplace)) {
                    occupation = (dataSet.getJobs().get(workplace));
                } else {
                    //logger.warn("Person " + id + " declared as worker does not have a valid job!");
                }
                break;
            case STUDENT:
                if(dataSet.getSchools().containsKey(school)) {
                    occupation = (dataSet.getSchools().get(school));
                } else {
                    //logger.warn("Person " + id + " declared as student does not have a valid school!");
                }
                break;
            case UNEMPLOYED:
            default:
                logger.debug("Person " + id + " does not have an occupation.");
                occupationCounter++;
                break;
        }

        MitoPerson pp = new MitoPerson7days(id, hh, mitoOccupationStatus, occupation, age, mitoGender, driversLicense);

        hh.addPerson(pp);
        dataSet.addPerson(pp);
    }
}
