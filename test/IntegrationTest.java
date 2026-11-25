import dao.*; import models.*; import util.Authorization;
import java.sql.*; import java.time.*; import java.util.*;
/**
 * Integration test for every DAO (modules 1-8) against a real database.
 * Run it right after loading a fresh sql/schema.sql (it relies on the sample data).
 *   javac -cp out -d out test/IntegrationTest.java
 *   java -cp "out:lib/*" -Ddb.password=YOURPASS IntegrationTest
 * Every line should say PASS; it ends with a pass/fail count.
 */
public class IntegrationTest {
  static int pass=0, fail=0;
  static void check(String name, boolean ok){ if(ok){pass++;System.out.println("PASS "+name);} else {fail++;System.out.println("FAIL "+name);} }
  interface Sql { void run() throws Exception; }
  static void expectFail(String name, Sql r, Class<?> type){ try{ r.run(); check(name+" (should fail)",false);}catch(Exception e){ check(name+" -> "+e.getClass().getSimpleName(), type.isInstance(e)); } }
  public static void main(String[] a) throws Exception {
    PatientDAO pd=new PatientDAO(); StaffDAO sd=new StaffDAO(); MedicalAssessmentDAO ad=new MedicalAssessmentDAO();
    LabTestDAO ld=new LabTestDAO(); VitalsDAO vd=new VitalsDAO(); TherapySessionDAO td=new TherapySessionDAO();
    MedicationDAO md=new MedicationDAO(); DischargePlanDAO dd=new DischargePlanDAO(); DashboardDAO dash=new DashboardDAO();
    // ---- modules 1-3 ----
    long pid=pd.register(new Patient(0,"Test Person","F",LocalDate.of(1995,3,3),"0700 111-222",null));
    check("patient register", pid>0 && pd.findById(pid).getContact().equals("0700111222"));
    check("patient search name", pd.search("test pers").size()==1);
    check("patient search contact", pd.search("0700111").size()==1);
    Patient p=pd.findById(pid); p.setFullName("Test Persona"); check("patient update", pd.update(p) && pd.findById(pid).getFullName().equals("Test Persona"));
    List<Staff> staff=sd.getAll(); Staff doc=staff.get(0), ther=staff.get(1), nurse=staff.get(2), recep=staff.get(3);
    check("staff polymorphism types", doc instanceof Doctor && ther instanceof Therapist && nurse instanceof Nurse && recep.getClass()==Staff.class);
    check("staff by role", sd.search("NURSE",true).size()==1);
    check("roles list", sd.getRoles().size()==4);
    long aid=ad.record(new MedicalAssessment(0,pid,doc.getId(),null,"None","Knee pain","Strain"));
    check("assessment record", aid>0);
    check("assessment by patient/staff/date", ad.findByPatient(pid).size()==1 && ad.findByStaff(doc.getId()).size()>=1 && ad.findByDate(LocalDate.now()).size()>=1);
    check("assessment combined search", ad.search(pid,doc.getId(),LocalDate.now()).size()==1 && ad.search(pid,ther.getId(),null).isEmpty());
    MedicalAssessment ma=ad.findById(aid); ma.setDiagnosis("Ligament strain"); check("assessment update", ad.update(ma) && ad.findById(aid).getDiagnosis().equals("Ligament strain"));
    // ---- module 4 lab ----
    long tid=ld.record(new LabTest(0,aid,"X-ray left knee",null,null,null));
    check("lab record pending", tid>0 && ld.findById(tid).isPending());
    expectFail("lab under missing assessment", () -> ld.record(new LabTest(0,99999,"CBC",null,null,null)), SQLException.class);
    check("lab by assessment", ld.findByAssessment(aid).size()==1);
    check("lab by patient (join)", ld.findByPatient(pid).size()==1 && ld.findByPatient(1).isEmpty());
    check("lab owner = assessor", ld.findOwnerStaffId(tid)==doc.getId());
    LabTest lt=ld.findById(tid); lt.setResult("No fracture"); lt.setNotes("Soft tissue swelling");
    check("lab update", ld.update(lt) && "Completed".equals(ld.findById(tid).getStatus()));
    // ---- module 5 vitals ----
    long v1=vd.record(new Vitals(0,pid,nurse.getId(),LocalDateTime.now().minusHours(3).withNano(0),36.6,70,16,5.2,118,76));
    long v2=vd.record(new Vitals(0,pid,nurse.getId(),null,38.4,108,22,12.5,150,95));
    check("vitals record", v1>0 && v2>0);
    Vitals latest=vd.findLatest(pid); check("vitals latest", latest.getVitalsId()==v2);
    check("vitals alerts on latest", latest.getAlerts().size()==5);
    check("vitals decimal round-trip", vd.findById(v1).getTemperature()==36.6 && vd.findById(v1).getGlucose()==5.2);
    check("vitals by patient/date/staff", vd.findByPatient(pid).size()==2 && vd.findByDate(LocalDate.now()).size()>=1 && vd.findByStaff(nurse.getId()).size()==2);
    check("vitals none on other date", vd.findByDate(LocalDate.of(2000,1,1)).isEmpty());
    // ---- module 6 therapy ----
    td.record(new TherapySession(0,pid,ther.getId(),LocalDateTime.now().minusDays(1),45,"Range of motion exercises"));
    td.record(new TherapySession(0,pid,ther.getId(),LocalDateTime.now().minusHours(1),50,"Strength training"));
    check("therapy history", td.findByPatient(pid).size()==2);
    check("therapy total minutes", td.totalMinutes(pid)==95 && TherapySession.formatMinutes(95).equals("1 h 35 min"));
    check("therapy total zero", td.totalMinutes(1)==0);
    check("therapy by staff / date", td.findByStaff(ther.getId()).size()==2 && td.findByDate(LocalDate.now().minusDays(1)).size()==1);
    // ---- module 7 medication ----
    long mid=md.record(new MedicationRecord(0,pid,doc.getId(),"Amoxicillin","500 mg","Three times daily","After meals",null));
    md.record(new MedicationRecord(0,pid,nurse.getId(),"Paracetamol","1 g","Every 8 hours",null,null));
    check("medication history", md.findByPatient(pid).size()==2);
    check("medication search name partial", md.searchByName("AMOX").size()==1);
    check("medication by staff", md.findByStaff(nurse.getId()).size()==1);
    MedicationRecord mr=md.findById(mid); mr.setDosage("250 mg"); mr.setInstructions(null);
    check("medication update", md.update(mr) && md.findById(mid).getDosage().equals("250 mg") && md.findById(mid).getInstructions()==null);
    // ---- module 8 discharge ----
    long plan=dd.create(new DischargePlan(0,pid,"Walk unaided","Daily exercises",null,null,"Jane Mother","0722 000 111",null));
    check("plan create, in treatment", plan>0 && dd.findById(plan).getStatus().equals("In treatment") && dd.findById(plan).getEmergencyContactPhone().equals("0722000111"));
    DischargePlan dp=dd.findById(plan); dp.setDates(LocalDate.now(), LocalDate.now().plusDays(14));
    check("plan dates", dd.updateDates(dp) && dd.findById(plan).getStatus().equals("Discharged"));
    check("upcoming follow-ups", dd.findUpcomingFollowUps().size()==1);
    dp.setRecoveryGoals("Walk 2 km unaided"); dp.setEmergencyContact("John Brother","0733000222");
    check("plan care update", dd.updateCarePlan(dp) && dd.findById(plan).getEmergencyContactName().equals("John Brother"));
    check("plans by patient", dd.findByPatient(pid).size()==1);
    dp.setDates(null,null); dd.updateDates(dp); check("plan dates cleared", dd.findById(plan).getDischargeDate()==null && dd.findUpcomingFollowUps().isEmpty());
    // ---- delete rules ----
    int linked=pd.countLinkedRecords(pid); check("patient linked count = 1+2+2+2+1 = 8", linked==8);
    expectFail("FK blocks patient delete", () -> pd.delete(pid), SQLIntegrityConstraintViolationException.class);
    check("staff linked counts", sd.countLinkedRecords(nurse.getId())==3 && sd.countLinkedRecords(recep.getId())==0);
    long tmp=pd.register(new Patient(0,"Temp Patient","M",LocalDate.of(2000,1,1),"0799999999",null));
    check("unlinked patient deletes", pd.countLinkedRecords(tmp)==0 && pd.delete(tmp) && pd.findById(tmp)==null);
    // ---- dashboard + auth ----
    check("dashboard counts", dash.countPatients()==3 && dash.countStaff()==4 && dash.countAssessments()==1 && dash.countAssessmentsOn(LocalDate.now())==1);
    check("auth: owner ok, doctor ok, other nurse no", Authorization.canEdit(nurse,nurse.getId()) && Authorization.canEdit(doc,nurse.getId()) && !Authorization.canEdit(ther,nurse.getId()));
    check("role permissions", nurse.canRecordVitals() && !ther.canRecordVitals() && ther.canConductTherapy() && !nurse.canConductTherapy() && doc.canManageMedication() && !recep.canManageMedication());
    System.out.println("\n"+pass+" passed, "+fail+" failed");
  }
}
