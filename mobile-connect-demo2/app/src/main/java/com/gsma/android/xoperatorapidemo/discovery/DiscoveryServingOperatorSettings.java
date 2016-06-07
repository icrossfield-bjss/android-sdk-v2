package com.gsma.android.xoperatorapidemo.discovery;


public class DiscoveryServingOperatorSettings {

    private static final ServingOperatorSetting sb1=new ServingOperatorSetting("Sb1 Sandbox (000-01)", false, "000", "01", null);
    private static final ServingOperatorSetting sb2=new ServingOperatorSetting("Sb2 Sandbox (000-02)", false, "000", "02", null);
      private static final ServingOperatorSetting auto=new ServingOperatorSetting("Auto (device MCC/MNC)", true, null, null, "5.5.5.5");
    private static final ServingOperatorSetting none=new ServingOperatorSetting("No auto assist", false, null, null, "5.5.5.5");

    private static final ServingOperatorSetting[] operators={auto, sb1, sb2, none};

    private static String[] operatorNames=null;

    static {
        operatorNames=new String[operators.length];
        int index=0;
        for (ServingOperatorSetting operator:operators) {
            operatorNames[index++]=operator.getName();
        }
    }

    public static String[] getOperatorNames() { return operatorNames; }

    public static ServingOperatorSetting getOperator(int index) { return operators[index]; }
}