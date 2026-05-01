import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DateNormalizer {

    private static final int MAX_DATE_LENGTH = 60;

    private static final Map<String, Integer> MONTH_NAMES = new LinkedHashMap<>();
    static {
        MONTH_NAMES.put("january",1);   MONTH_NAMES.put("jan",1);
        MONTH_NAMES.put("february",2);  MONTH_NAMES.put("feb",2);
        MONTH_NAMES.put("march",3);     MONTH_NAMES.put("mar",3);
        MONTH_NAMES.put("april",4);     MONTH_NAMES.put("apr",4);
        MONTH_NAMES.put("may",5);
        MONTH_NAMES.put("june",6);      MONTH_NAMES.put("jun",6);
        MONTH_NAMES.put("july",7);      MONTH_NAMES.put("jul",7);
        MONTH_NAMES.put("august",8);    MONTH_NAMES.put("aug",8);
        MONTH_NAMES.put("september",9); MONTH_NAMES.put("sep",9);
        MONTH_NAMES.put("sept",9);
        MONTH_NAMES.put("october",10);  MONTH_NAMES.put("oct",10);
        MONTH_NAMES.put("november",11); MONTH_NAMES.put("nov",11);
        MONTH_NAMES.put("december",12); MONTH_NAMES.put("dec",12);
    }

    private static final int ROLE_YEAR  = 0;
    private static final int ROLE_MONTH = 1;
    private static final int ROLE_DAY   = 2;

    private static final Map<String, int[]> ORDER_MAP = new HashMap<>();
    static {
        ORDER_MAP.put("YMD", new int[]{ROLE_YEAR,  ROLE_MONTH, ROLE_DAY});
        ORDER_MAP.put("MDY", new int[]{ROLE_MONTH, ROLE_DAY,   ROLE_YEAR});
        ORDER_MAP.put("DMY", new int[]{ROLE_DAY,   ROLE_MONTH, ROLE_YEAR});
    }

    private static final String SEP = "[/\\\\\\-\\.\\s]+";

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC ENTRY POINTS
    // ─────────────────────────────────────────────────────────────────────────

    // Full signature — secondaryOrder is optional, pass null if not needed
    public String normalize(String input, String inputOrder, String outputFormat,
                            int suspiciousYearBefore, int minYear, int maxYear,
                            String secondaryOrder) {
        if (input == null || input.trim().isEmpty()) return input;

        String trimmed = input.trim();
        if (trimmed.length() > MAX_DATE_LENGTH)
            return "INVALID_DATE: " + trimmed;

        // Fast pre-screen: reject strings with no digits at all
        boolean hasDigit = false;
        for (int ci = 0; ci < trimmed.length(); ci++) {
            if (Character.isDigit(trimmed.charAt(ci))) { hasDigit = true; break; }
        }
        if (!hasDigit) return "INVALID_DATE: " + trimmed + " (no numeric content)";

        String cleaned = trimmed
                .replaceAll("(?<=\\d)(st|nd|rd|th)", "")
                .replace(",", "")
                .replaceAll("\\s+", " ")
                .trim();

        // Strip time components
        cleaned = cleaned.replaceAll(
                "T\\d{2}:\\d{2}:\\d{2}(Z|[+\\-]\\d{2}:\\d{2})?$", "").trim();
        cleaned = cleaned.replaceAll(
                "\\s+\\d{2}:\\d{2}(:\\d{2})?$", "").trim();

        // Compact 8-digit
        if (cleaned.matches("\\d{8}"))
            return handleCompact(cleaned, outputFormat, minYear, maxYear,
                    suspiciousYearBefore, trimmed);
        if (cleaned.matches("\\d{5,7}") || cleaned.matches("\\d{9,}"))
            return "INVALID_DATE: " + trimmed + " (unrecognized compact format)";

        // Split and filter empty tokens
        String[] allTokens = cleaned.split(SEP);
        List<String> tokenList = new ArrayList<>();
        for (String t : allTokens) if (!t.trim().isEmpty()) tokenList.add(t.trim());

        // Pre-clean: strip junk words, extract numeric from mixed tokens
        List<String> cleanedTokens = new ArrayList<>();
        for (String tok : tokenList) {
            if (MONTH_NAMES.containsKey(tok.toLowerCase())) { cleanedTokens.add(tok); continue; }
            if (tok.matches("\\d+"))                        { cleanedTokens.add(tok); continue; }
            String numOnly = tok.replaceAll("[^0-9]", "");
            if (!numOnly.isEmpty() && numOnly.length() <= 4) { cleanedTokens.add(numOnly); continue; }
            if (tok.matches("[a-zA-Z]+")) continue; // discard junk word
        }

        String[] rawTokens = cleanedTokens.toArray(new String[0]);

        // Separate text months from numeric
        List<String> textMonths  = new ArrayList<>();
        List<String> numericToks = new ArrayList<>();
        for (String tok : rawTokens) {
            if (MONTH_NAMES.containsKey(tok.toLowerCase())) textMonths.add(tok);
            else numericToks.add(tok);
        }

        if (!textMonths.isEmpty())
            return resolveWithTextMonth(textMonths.get(0), numericToks,
                    outputFormat, minYear, maxYear, suspiciousYearBefore, trimmed);

        if (rawTokens.length != 3)
            return "INVALID_DATE: " + trimmed
                    + " (expected 3 date components, found " + rawTokens.length + ")";

        // Pass secondaryOrder into resolveNumeric
        return resolveNumeric(rawTokens, inputOrder, outputFormat,
                minYear, maxYear, suspiciousYearBefore, trimmed, secondaryOrder);
    }

    // Legacy entry points for backward compatibility
    public String normalize(String input, String inputOrder, String outputFormat,
                            int suspiciousYearBefore, int minYear, int maxYear) {
        return normalize(input, inputOrder, outputFormat,
                suspiciousYearBefore, minYear, maxYear, null);
    }

    public String normalize(String input, String inputOrder,
                            String outputFormat, int suspiciousYearBefore) {
        return normalize(input, inputOrder, outputFormat,
                suspiciousYearBefore, 1900, 2100, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COMPACT 8-DIGIT
    // ─────────────────────────────────────────────────────────────────────────
    private String handleCompact(String s, String outputFormat,
                                 int minYear, int maxYear,
                                 int suspiciousYearBefore, String original) {
        int y1=Integer.parseInt(s.substring(0,4));
        int m1=Integer.parseInt(s.substring(4,6));
        int d1=Integer.parseInt(s.substring(6,8));
        int y2=Integer.parseInt(s.substring(4,8));
        int m2=Integer.parseInt(s.substring(2,4));
        int d2=Integer.parseInt(s.substring(0,2));

        boolean yyyymmdd=isValidDate(y1,m1,d1)&&y1>=minYear&&y1<=maxYear;
        boolean ddmmyyyy=isValidDate(y2,m2,d2)&&y2>=minYear&&y2<=maxYear;

        if (yyyymmdd&&!ddmmyyyy) {
            if (suspiciousYearBefore>0&&y1<suspiciousYearBefore)
                return "SUSPICIOUS_DATE: "+original+" (year "+y1
                        +" before threshold "+suspiciousYearBefore+")";
            return format(LocalDate.of(y1,m1,d1),outputFormat);
        }
        if (yyyymmdd)
            return "SUSPICIOUS_DATE: "+original
                    +" (ambiguous compact date — could be YYYYMMDD or DDMMYYYY)";
        return "INVALID_DATE: "+original+" (no valid interpretation of compact date)";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RESOLVE WITH TEXT MONTH
    // ─────────────────────────────────────────────────────────────────────────
    private String resolveWithTextMonth(String monthToken, List<String> numericToks,
                                        String outputFormat, int minYear, int maxYear,
                                        int suspiciousYearBefore, String original) {
        int month=MONTH_NAMES.get(monthToken.toLowerCase());
        if (numericToks.size()!=2)
            return "INVALID_DATE: "+original+" (expected 2 numeric components with text month)";

        Integer aVal=parseIntSafe(numericToks.get(0).trim());
        Integer bVal=parseIntSafe(numericToks.get(1).trim());
        if (aVal==null||bVal==null)
            return "INVALID_DATE: "+original+" (non-numeric component)";

        int aLen=numericToks.get(0).trim().length();
        int bLen=numericToks.get(1).trim().length();
        Integer year=null,day=null;

        if (aLen==4&&bLen!=4)      {year=aVal;day=bVal;}
        else if (bLen==4&&aLen!=4) {year=bVal;day=aVal;}
        else if (aLen==4)
            return "SUSPICIOUS_DATE: "+original+" (ambiguous — two 4-digit numbers with text month)";
        else {
            if (aVal>31&&bVal<=31)      {year=aVal+2000;day=bVal;}
            else if (bVal>31&&aVal<=31) {year=bVal+2000;day=aVal;}
            else return "SUSPICIOUS_DATE: "+original
                        +" (ambiguous — cannot determine year vs day with text month)";
        }

        if (year!=null&&year<100) year+=2000;
        if (!isValidDate(year,month,day))
            return "INVALID_DATE: "+original
                    +" (invalid date: "+year+"-"+pad(month)+"-"+pad(day)+")";
        if (year<minYear||year>maxYear)
            return "SUSPICIOUS_DATE: "+original
                    +" (year "+year+" outside range "+minYear+"-"+maxYear+")";
        if (suspiciousYearBefore>0&&year<suspiciousYearBefore)
            return "SUSPICIOUS_DATE: "+original
                    +" (year "+year+" before threshold "+suspiciousYearBefore+")";
        return format(LocalDate.of(year,month,day),outputFormat);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RESOLVE PURELY NUMERIC 3-TOKEN DATE
    // secondaryOrder: optional — when two valid interpretations exist,
    // secondary breaks the tie and returns a clean date
    // ─────────────────────────────────────────────────────────────────────────
    private String resolveNumeric(String[] tokens, String inputOrder,
                                  String outputFormat, int minYear, int maxYear,
                                  int suspiciousYearBefore, String original,
                                  String secondaryOrder) {
        Integer[] vals=new Integer[3];
        int[] lens=new int[3];

        for (int i=0;i<3;i++) {
            vals[i]=parseIntSafe(tokens[i]);
            lens[i]=tokens[i].length();
            if (vals[i]==null)
                return "INVALID_DATE: "+original+" (non-numeric token: "+tokens[i]+")";
        }

        for (int i=0;i<3;i++) {
            if (lens[i]==3||lens[i]>=5)
                return "INVALID_DATE: "+original
                        +" (token '"+tokens[i]+"' has invalid length "+lens[i]+")";
        }

        // Find 4-digit year candidates
        List<Integer> fourDigitIndexes=new ArrayList<>();
        for (int i=0;i<3;i++) if (lens[i]==4) fourDigitIndexes.add(i);
        if (fourDigitIndexes.size()>1)
            return "INVALID_DATE: "+original+" (multiple 4-digit numbers)";

        int[] preferredOrder=ORDER_MAP.getOrDefault(
                inputOrder.toUpperCase(),ORDER_MAP.get("MDY"));

        // ─────────────────────────────────────────────────────────────────────
        // CASE A: One 4-digit year token
        // ─────────────────────────────────────────────────────────────────────
        if (fourDigitIndexes.size()==1) {
            int yearIdx=fourDigitIndexes.get(0);
            int year=vals[yearIdx];

            List<Integer> rest=new ArrayList<>();
            for (int i=0;i<3;i++) if (i!=yearIdx) rest.add(i);
            int ia=rest.get(0),ib=rest.get(1);
            int va=vals[ia],vb=vals[ib];

            boolean abValid=va>=1&&va<=12&&isValidDate(year,va,vb);
            boolean baValid=vb>=1&&vb<=12&&isValidDate(year,vb,va);

            if (!abValid&&!baValid)
                return "INVALID_DATE: "+original
                        +" (no valid month/day combination for year "+year+")";
            if (year<minYear||year>maxYear)
                return "SUSPICIOUS_DATE: "+original
                        +" (year "+year+" outside range "+minYear+"-"+maxYear+")";

            // Only one valid arrangement
            if (abValid&&!baValid) {
                // Check if the original month slot had a value > 12 (swapped)
                int[] nonYearPositions=new int[2]; int nyp=0;
                for (int pos=0;pos<3;pos++) if (preferredOrder[pos]!=ROLE_YEAR) nonYearPositions[nyp++]=pos;
                int intendedMonthVal=(preferredOrder[nonYearPositions[0]]==ROLE_MONTH)?va:vb;
                boolean wasSwapped=intendedMonthVal>12;
                if (wasSwapped)
                    return "SUSPICIOUS_DATE: "+original
                            +" (month/day appear swapped — assumed correction: "
                            +format(LocalDate.of(year,va,vb),outputFormat)+")";
                if (suspiciousYearBefore>0&&year<suspiciousYearBefore)
                    return "SUSPICIOUS_DATE: "+original
                            +" (year "+year+" before threshold "+suspiciousYearBefore+")";
                return format(LocalDate.of(year,va,vb),outputFormat);
            }
            if (baValid&&!abValid) {
                int[] nonYearPositions=new int[2]; int nyp=0;
                for (int pos=0;pos<3;pos++) if (preferredOrder[pos]!=ROLE_YEAR) nonYearPositions[nyp++]=pos;
                int intendedMonthVal=(preferredOrder[nonYearPositions[0]]==ROLE_MONTH)?va:vb;
                boolean wasSwapped=intendedMonthVal>12;
                if (wasSwapped)
                    return "SUSPICIOUS_DATE: "+original
                            +" (month/day appear swapped — assumed correction: "
                            +format(LocalDate.of(year,vb,va),outputFormat)+")";
                if (suspiciousYearBefore>0&&year<suspiciousYearBefore)
                    return "SUSPICIOUS_DATE: "+original
                            +" (year "+year+" before threshold "+suspiciousYearBefore+")";
                return format(LocalDate.of(year,vb,va),outputFormat);
            }

            // Both valid — use preferred order
            int preferredYearPosition=-1;
            for (int pos=0;pos<3;pos++) if (preferredOrder[pos]==ROLE_YEAR) preferredYearPosition=pos;
            boolean yearInPreferredPosition=(yearIdx==preferredYearPosition);

            int[] nonYearRoles=new int[2]; int nyrCount=0;
            for (int pos=0;pos<3;pos++)
                if (preferredOrder[pos]!=ROLE_YEAR) nonYearRoles[nyrCount++]=preferredOrder[pos];
            int preferredMonth=nonYearRoles[0]==ROLE_MONTH?va:vb;
            int preferredDay  =nonYearRoles[0]==ROLE_MONTH?vb:va;

            if (preferredMonth>=1&&preferredMonth<=12
                    &&isValidDate(year,preferredMonth,preferredDay)) {
                LocalDate preferred=LocalDate.of(year,preferredMonth,preferredDay);
                LocalDate other=preferred.equals(LocalDate.of(year,va,vb))
                        ?LocalDate.of(year,vb,va):LocalDate.of(year,va,vb);

                if (preferred.equals(other)) {
                    if (suspiciousYearBefore>0&&year<suspiciousYearBefore)
                        return "SUSPICIOUS_DATE: "+original
                                +" (year "+year+" before threshold "+suspiciousYearBefore+")";
                    return format(preferred,outputFormat);
                }

                if (yearInPreferredPosition) {
                    if (suspiciousYearBefore>0&&year<suspiciousYearBefore)
                        return "SUSPICIOUS_DATE: "+original
                                +" (year "+year+" before threshold "+suspiciousYearBefore+")";
                    return format(preferred,outputFormat);
                }

                // Year NOT in preferred position — check secondary order
                if (secondaryOrder!=null&&!secondaryOrder.isEmpty()) {
                    int[] secArr=ORDER_MAP.getOrDefault(secondaryOrder.toUpperCase(),null);
                    if (secArr!=null) {
                        int[] secNonYearRoles=new int[2]; int sny=0;
                        for (int pos=0;pos<3;pos++)
                            if (secArr[pos]!=ROLE_YEAR) secNonYearRoles[sny++]=secArr[pos];
                        int secMonth=secNonYearRoles[0]==ROLE_MONTH?va:vb;
                        int secDay  =secNonYearRoles[0]==ROLE_MONTH?vb:va;
                        if (secMonth>=1&&secMonth<=12&&isValidDate(year,secMonth,secDay)) {
                            LocalDate secDate=LocalDate.of(year,secMonth,secDay);
                            if (suspiciousYearBefore>0&&year<suspiciousYearBefore)
                                return "SUSPICIOUS_DATE: "+original
                                        +" (year "+year+" before threshold "+suspiciousYearBefore+")";
                            return format(secDate,outputFormat);
                        }
                    }
                }

                return "SUSPICIOUS_DATE: "+original
                        +" (preferred "+inputOrder+" gives "+preferred
                        +", also valid: "+other+")";
            }

            LocalDate fallback=abValid?LocalDate.of(year,va,vb):LocalDate.of(year,vb,va);
            if (suspiciousYearBefore>0&&year<suspiciousYearBefore)
                return "SUSPICIOUS_DATE: "+original
                        +" (year "+year+" before threshold "+suspiciousYearBefore+")";
            return format(fallback,outputFormat);
        }

        // ─────────────────────────────────────────────────────────────────────
        // CASE B: No 4-digit year
        // Use largest value as year candidate (32-99 forced year).
        // Always suspicious — 2-digit year is structurally malformed.
        // ─────────────────────────────────────────────────────────────────────
        int forcedYearTokenIdx=-1;
        for (int i=0;i<3;i++) {
            if (vals[i]>=32&&vals[i]<=99) {
                if (forcedYearTokenIdx!=-1)
                    return "SUSPICIOUS_DATE: "+original
                            +" (multiple values that can only be 2-digit years)";
                forcedYearTokenIdx=i;
            }
        }

        int yearTokenIdx=forcedYearTokenIdx;
        if (yearTokenIdx==-1) {
            int maxVal=Integer.MIN_VALUE;
            for (int i=0;i<3;i++) if (vals[i]>maxVal) {maxVal=vals[i];yearTokenIdx=i;}
        }

        int year2d=vals[yearTokenIdx];
        int fullYear=year2d<100?year2d+2000:year2d;

        if (fullYear<minYear||fullYear>maxYear)
            return "SUSPICIOUS_DATE: "+original
                    +" (2-digit year assumed as "+fullYear
                    +" but outside range "+minYear+"-"+maxYear+")";

        List<Integer> remaining=new ArrayList<>();
        for (int i=0;i<3;i++) if (i!=yearTokenIdx) remaining.add(i);
        int ia=remaining.get(0),ib=remaining.get(1);
        int va=vals[ia],vb=vals[ib];

        boolean abValid=va>=1&&va<=12&&isValidDate(fullYear,va,vb);
        boolean baValid=vb>=1&&vb<=12&&isValidDate(fullYear,vb,va);

        if (!abValid&&!baValid)
            return "INVALID_DATE: "+original
                    +" (no valid month/day combination for 2-digit year "+fullYear+")";

        String yearNote=suspiciousYearBefore>0&&fullYear<suspiciousYearBefore
                ?", year before threshold "+suspiciousYearBefore:"";

        if (abValid&&!baValid)
            return "INVALID_DATE: "+original
                    +" (malformed date — 2-digit year assumed "+fullYear
                    +", only interpretation: "+LocalDate.of(fullYear,va,vb)+yearNote+")";
        if (baValid&&!abValid)
            return "INVALID_DATE: "+original
                    +" (malformed date — 2-digit year assumed "+fullYear
                    +", only interpretation: "+LocalDate.of(fullYear,vb,va)+yearNote+")";

        LocalDate opt1=LocalDate.of(fullYear,va,vb);
        LocalDate opt2=LocalDate.of(fullYear,vb,va);
        if (opt1.equals(opt2))
            return "INVALID_DATE: "+original
                    +" (malformed date — 2-digit year assumed "+fullYear
                    +", interpretation: "+opt1+yearNote+")";

        // Multiple interpretations — check secondary order to break tie
        if (secondaryOrder!=null&&!secondaryOrder.isEmpty()) {
            int[] secArr=ORDER_MAP.getOrDefault(secondaryOrder.toUpperCase(),null);
            if (secArr!=null) {
                int[] secNonYear=new int[2]; int sny=0;
                for (int pos=0;pos<3;pos++)
                    if (secArr[pos]!=ROLE_YEAR) secNonYear[sny++]=secArr[pos];
                int secMonth=secNonYear[0]==ROLE_MONTH?va:vb;
                int secDay  =secNonYear[0]==ROLE_MONTH?vb:va;
                if (secMonth>=1&&secMonth<=12&&isValidDate(fullYear,secMonth,secDay)) {
                    LocalDate secDate=LocalDate.of(fullYear,secMonth,secDay);
                    if (suspiciousYearBefore>0&&fullYear<suspiciousYearBefore)
                        return "SUSPICIOUS_DATE: "+original
                                +" (year "+fullYear+" before threshold "+suspiciousYearBefore+")";
                    return format(secDate,outputFormat);
                }
            }
        }

        return "SUSPICIOUS_DATE: "+original
                +" (2-digit year, multiple interpretations: "
                +opt1+", "+opt2+yearNote+")";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isValidDate(int year,int month,int day) {
        if (month<1||month>12) return false;
        if (day<1||day>31) return false;
        try { return day<=YearMonth.of(year,month).lengthOfMonth(); }
        catch (Exception e) { return false; }
    }

    private Integer parseIntSafe(String s) {
        if (s==null||s.isEmpty()) return null;
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return null; }
    }

    private String format(LocalDate date,String outputFormat) {
        try { return date.format(DateTimeFormatter.ofPattern(outputFormat,Locale.ENGLISH)); }
        catch (Exception e) { return date.toString(); }
    }

    private String pad(int n) { return n<10?"0"+n:String.valueOf(n); }
}