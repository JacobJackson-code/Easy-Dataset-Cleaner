import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class NumberNormalizer {

    // ── Scientific notation setting ───────────────────────────────────────────
    private boolean allowScientificNotation = false;

    public void setAllowScientificNotation(boolean allow) {
        this.allowScientificNotation = allow;
    }

    // ── Currency symbols ──────────────────────────────────────────────────────
    private static final Map<String, String> CURRENCY_SYMBOLS = new LinkedHashMap<>();
    private static final Set<String> KNOWN_CURRENCY_CODES = new HashSet<>();

    public enum FormatStyle { STANDARD, EUROPEAN, SWISS, INDIAN }

    private static final Map<String, FormatStyle> CURRENCY_STYLES   = new HashMap<>();
    private static final Map<String, Integer>     CURRENCY_DECIMALS = new HashMap<>();

    // ── Extended boolean map ──────────────────────────────────────────────────
    private static final Map<String, Boolean> BOOL_MAP = new HashMap<>();

    static {
        CURRENCY_SYMBOLS.put("USD","$");    CURRENCY_SYMBOLS.put("EUR","€");
        CURRENCY_SYMBOLS.put("GBP","£");    CURRENCY_SYMBOLS.put("JPY","¥");
        CURRENCY_SYMBOLS.put("INR","₹");    CURRENCY_SYMBOLS.put("CAD","CA$");
        CURRENCY_SYMBOLS.put("AUD","A$");   CURRENCY_SYMBOLS.put("CHF","CHF ");
        CURRENCY_SYMBOLS.put("MXN","MX$");  CURRENCY_SYMBOLS.put("BRL","R$");
        CURRENCY_SYMBOLS.put("CNY","¥");    CURRENCY_SYMBOLS.put("KRW","₩");
        CURRENCY_SYMBOLS.put("SGD","S$");   CURRENCY_SYMBOLS.put("HKD","HK$");
        CURRENCY_SYMBOLS.put("NOK","kr");   CURRENCY_SYMBOLS.put("SEK","kr");
        CURRENCY_SYMBOLS.put("DKK","kr");   CURRENCY_SYMBOLS.put("NZD","NZ$");
        CURRENCY_SYMBOLS.put("ZAR","R");    CURRENCY_SYMBOLS.put("KWD","KD");
        CURRENCY_SYMBOLS.put("BHD","BD");   CURRENCY_SYMBOLS.put("OMR","OMR");
        CURRENCY_SYMBOLS.put("JOD","JD");   CURRENCY_SYMBOLS.put("TND","DT");
        CURRENCY_SYMBOLS.put("IQD","IQD");  CURRENCY_SYMBOLS.put("LYD","LD");
        KNOWN_CURRENCY_CODES.addAll(CURRENCY_SYMBOLS.keySet());

        for (String c : Arrays.asList("EUR","NOK","SEK","DKK","ISK"))
            CURRENCY_STYLES.put(c, FormatStyle.EUROPEAN);
        CURRENCY_STYLES.put("CHF", FormatStyle.SWISS);
        CURRENCY_STYLES.put("INR", FormatStyle.INDIAN);

        for (String c : Arrays.asList(
                "JPY","KRW","VND","IDR","HUF","CLP","ISK","PYG",
                "UGX","RWF","GNF","KMF","MGA","DJF","BIF","XOF","XAF","XPF"))
            CURRENCY_DECIMALS.put(c, 0);
        for (String c : Arrays.asList("KWD","BHD","OMR","JOD","TND","IQD","LYD"))
            CURRENCY_DECIMALS.put(c, 3);

        // ── Extended boolean map ──────────────────────────────────────────────
        // Truthy values
        for (String s : Arrays.asList(
                "true","yes","1","on","enabled","active","y","t",
                "ok","positive","correct","valid",
                "confirm","confirmed","accept","accepted","approved",
                "agree","agreed","success","succeeded","successful",
                "open","pass","passed","complete","completed","done",
                "allow","allowed","available","checked","selected",
                "present","exists","found","match","matched","grant","granted"))
            BOOL_MAP.put(s, Boolean.TRUE);

        // Falsy values
        for (String s : Arrays.asList(
                "false","no","0","off","disabled","inactive","n","f",
                "negative","incorrect","invalid",
                "deny","denied","reject","rejected","declined","decline",
                "disagree","fail","failed","failure","unsuccessful",
                "closed","block","blocked","unavailable","unchecked",
                "absent","missing","not found","none","null",
                "error","revoke","revoked","cancel","cancelled","canceled",
                "pending","void","voided","expired"))
            BOOL_MAP.put(s, Boolean.FALSE);
    }

    // ── Name suffixes / prefixes ──────────────────────────────────────────────
    private static final Map<String, String> NAME_SUFFIXES = new LinkedHashMap<>();
    private static final Set<String> NAME_PREFIXES = new HashSet<>(Arrays.asList(
            "mr","mr.","mrs","mrs.","ms","ms.","miss","dr","dr.",
            "prof","prof.","rev","rev."));
    static {
        NAME_SUFFIXES.put("jr","Jr.");   NAME_SUFFIXES.put("jr.","Jr.");
        NAME_SUFFIXES.put("sr","Sr.");   NAME_SUFFIXES.put("sr.","Sr.");
        NAME_SUFFIXES.put("ii","II");    NAME_SUFFIXES.put("iii","III");
        NAME_SUFFIXES.put("iv","IV");    NAME_SUFFIXES.put("esq","Esq.");
        NAME_SUFFIXES.put("esq.","Esq."); NAME_SUFFIXES.put("md","MD");
        NAME_SUFFIXES.put("phd","PhD");  NAME_SUFFIXES.put("dds","DDS");
    }

    private static final Map<String, Long> WORD_MAP = new LinkedHashMap<>();
    static {
        WORD_MAP.put("zero",0L); WORD_MAP.put("one",1L); WORD_MAP.put("two",2L);
        WORD_MAP.put("three",3L); WORD_MAP.put("four",4L); WORD_MAP.put("five",5L);
        WORD_MAP.put("six",6L); WORD_MAP.put("seven",7L); WORD_MAP.put("eight",8L);
        WORD_MAP.put("nine",9L); WORD_MAP.put("ten",10L); WORD_MAP.put("eleven",11L);
        WORD_MAP.put("twelve",12L); WORD_MAP.put("thirteen",13L);
        WORD_MAP.put("fourteen",14L); WORD_MAP.put("fifteen",15L);
        WORD_MAP.put("sixteen",16L); WORD_MAP.put("seventeen",17L);
        WORD_MAP.put("eighteen",18L); WORD_MAP.put("nineteen",19L);
        WORD_MAP.put("twenty",20L); WORD_MAP.put("thirty",30L);
        WORD_MAP.put("forty",40L); WORD_MAP.put("fifty",50L);
        WORD_MAP.put("sixty",60L); WORD_MAP.put("seventy",70L);
        WORD_MAP.put("eighty",80L); WORD_MAP.put("ninety",90L);
        WORD_MAP.put("hundred",100L); WORD_MAP.put("thousand",1000L);
        WORD_MAP.put("million",1000000L);
    }

    private static final String NEG_STANDARD = "standard";
    private static final String NEG_PARENS   = "parens";
    private static final String NEG_FLAG     = "flag";

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC ENTRY POINTS
    // ─────────────────────────────────────────────────────────────────────────

    public String apply(String input, String rule) { return apply(input, rule, null); }

    public String apply(String input, String rule, Long cap) {
        if (input==null||input.trim().isEmpty()) return input;
        if (rule==null||rule.trim().isEmpty()) return input;

        String t=input.trim();
        if (t.startsWith("INVALID_")||t.startsWith("CAP_EXCEEDED:")
                ||t.startsWith("NULL_VALUE:")||t.startsWith("REJECTED:")
                ||t.startsWith("SUSPICIOUS_DATE:")) return input;

        String trimmed=t.replaceAll("^\"+|\"+$","");
        if (trimmed.isEmpty()) return input;
        if (trimmed.equals("∞")||trimmed.equals("-∞")
                ||trimmed.equalsIgnoreCase("NaN")
                ||trimmed.equalsIgnoreCase("infinity")
                ||trimmed.equalsIgnoreCase("-infinity"))
            return "INVALID_NUMBER: "+input;
        input=trimmed;

        String[] parts=rule.trim().split(":");
        String type=parts[0].toLowerCase();

        String negStyle=NEG_FLAG;
        for (String p : parts) {
            String pl=p.toLowerCase();
            if (pl.equals(NEG_STANDARD)){negStyle=NEG_STANDARD;break;}
            if (pl.equals(NEG_PARENS))  {negStyle=NEG_PARENS;  break;}
            if (pl.equals(NEG_FLAG))    {negStyle=NEG_FLAG;    break;}
        }

        boolean isParens=isParensNotation(input);
        if (isParens) {
            if (negStyle.equals(NEG_FLAG)) return "INVALID_NUMBER: "+input;
            input=parensToNegative(input);
        }

        String result;
        switch (type) {
            case "integer":
                result=toInteger(input);
                if (result==null) return "INVALID_NUMBER: "+input;
                break;
            case "words":
                result=toWords(input);
                break;
            case "currency":
                result=formatCurrency(input,toLower(parts));
                if (result==null) return "INVALID_NUMBER: "+input;
                break;
            case "decimal":
                result=formatDecimal(input,toLower(parts));
                if (result==null) return "INVALID_NUMBER: "+input;
                break;
            case "round":
                result=formatRound(input,toLower(parts));
                if (result==null) return "INVALID_NUMBER: "+input;
                break;
            case "percent":
                result=formatPercent(input,toLower(parts));
                if (result==null) return "INVALID_NUMBER: "+input;
                break;
            case "case":          return applyCase(input,toLower(parts));
            case "currency_code": return applyCurrencyCode(input,toLower(parts));
            case "name":          return applyName(input,toLower(parts));
            case "validate":      return applyValidate(input,toLower(parts));
            case "id":            return applyId(input,parts);
            case "bool":          return applyBool(input,parts);
            default:              return input;
        }

        if (negStyle.equals(NEG_PARENS)&&result!=null)
            result=applyParensOutput(result);

        if (cap!=null&&result!=null) {
            BigDecimal parsed=parseNumber(result);
            if (parsed!=null&&parsed.compareTo(BigDecimal.valueOf(cap))>0)
                return "CAP_EXCEEDED: "+result;
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CURRENCY-AWARE FORMATTING
    // ─────────────────────────────────────────────────────────────────────────
    public String formatForCurrency(String input, String currencyCode, String mode) {
        if (input==null||input.trim().isEmpty()) return input;
        String t=input.trim();
        if (isDirty(t)) return input;
        BigDecimal value=parseNumber(input);
        if (value==null) return "INVALID_NUMBER: "+input;
        String code=currencyCode.toUpperCase().trim();
        int decimals=CURRENCY_DECIMALS.getOrDefault(code,2);
        switch (mode.toLowerCase()) {
            case "decimal":
                return decimals>0
                        ?String.format("%."+decimals+"f",value.setScale(decimals,RoundingMode.HALF_UP))
                        :String.valueOf(value.setScale(0,RoundingMode.HALF_UP).longValue());
            case "nogrouping":
                return formatByStyle(value,decimals,CURRENCY_STYLES.getOrDefault(code,FormatStyle.STANDARD),false);
            default:
                return formatByStyle(value,decimals,CURRENCY_STYLES.getOrDefault(code,FormatStyle.STANDARD),true);
        }
    }

    private boolean isDirty(String cell) {
        if (cell==null||cell.trim().isEmpty()) return true;
        String t=cell.trim();
        return t.startsWith("INVALID_")||t.startsWith("CAP_EXCEEDED:")
                ||t.startsWith("NULL_VALUE:")||t.startsWith("REJECTED:")
                ||t.startsWith("SUSPICIOUS_DATE:");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FORMAT BY STYLE
    // ─────────────────────────────────────────────────────────────────────────
    private String formatByStyle(BigDecimal value,int decimals,FormatStyle style,boolean useThousands) {
        BigDecimal scaled=value.setScale(decimals,RoundingMode.HALF_UP);
        switch (style) {
            case EUROPEAN: {
                if (!useThousands)
                    return decimals>0?String.format("%."+decimals+"f",scaled).replace(".",","):String.valueOf(scaled.longValue());
                DecimalFormatSymbols sym=new DecimalFormatSymbols();
                sym.setGroupingSeparator('.'); sym.setDecimalSeparator(',');
                return new DecimalFormat(buildPattern(decimals),sym).format(scaled);
            }
            case SWISS: {
                if (!useThousands)
                    return decimals>0?String.format("%."+decimals+"f",scaled):String.valueOf(scaled.longValue());
                DecimalFormatSymbols sym=new DecimalFormatSymbols();
                sym.setGroupingSeparator('\''); sym.setDecimalSeparator('.');
                return new DecimalFormat(buildPattern(decimals),sym).format(scaled);
            }
            case INDIAN: return formatIndian(scaled,decimals,useThousands);
            default: {
                if (!useThousands)
                    return decimals>0?String.format("%."+decimals+"f",scaled):String.valueOf(scaled.longValue());
                return new DecimalFormat(buildPattern(decimals),DecimalFormatSymbols.getInstance(Locale.US)).format(scaled);
            }
        }
    }

    private String buildPattern(int decimals) {
        StringBuilder p=new StringBuilder("#,##0");
        if (decimals>0){p.append(".");for(int i=0;i<decimals;i++)p.append("0");}
        return p.toString();
    }

    private String formatIndian(BigDecimal value,int decimals,boolean useThousands) {
        String plain=value.toPlainString();
        String intPart; String decPart="";
        if (plain.contains(".")){intPart=plain.substring(0,plain.indexOf('.'));decPart=plain.substring(plain.indexOf('.'));}
        else{intPart=plain;}
        if (decimals>0){while(decPart.length()-1<decimals)decPart+="0";if(decPart.isEmpty())decPart="."+String.valueOf('0').repeat(decimals);}
        else{decPart="";}
        if (!useThousands||intPart.length()<=3) return intPart+decPart;
        StringBuilder result=new StringBuilder();
        result.insert(0,intPart.substring(intPart.length()-3));
        intPart=intPart.substring(0,intPart.length()-3);
        while(intPart.length()>2){result.insert(0,intPart.substring(intPart.length()-2)+",");intPart=intPart.substring(0,intPart.length()-2);}
        if (!intPart.isEmpty()) result.insert(0,intPart+",");
        return result.toString()+decPart;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NAME NORMALIZATION
    // Modes: full / first / last / initial_last / initialnodot_last
    // ─────────────────────────────────────────────────────────────────────────
    private String applyName(String input, String[] parts) {
        String mode=(parts.length>1)?parts[1]:"full";
        String cleaned=input.trim()
                .replace(";",",").replace("|",",").replace("/",",")
                .replaceAll("[^a-zA-Z\\s,.'\\-]","").replaceAll("\\s+"," ").trim();
        if (cleaned.contains(",")) {
            String[] cp=cleaned.split(",",2);
            if (cp.length==2) cleaned=cp[1].trim()+" "+cp[0].trim();
        }
        // Split compact initials: C.Lee → C. Lee  c.lee → c. lee
        // Negative lookbehind ensures we only split a true initial, not mid-word
        cleaned=cleaned.replaceAll("(?i)(?<![a-zA-Z])([a-zA-Z])\\.([a-zA-Z]{2,})","$1. $2");
        String[] tokens=cleaned.split("\\s+");
        List<String> nameTokens=new ArrayList<>();
        String suffix=null;
        for (String token : tokens) {
            String lower=token.toLowerCase().replaceAll("\\.$","");
            if (NAME_PREFIXES.contains(lower)||NAME_PREFIXES.contains(lower+".")) continue;
            if (NAME_SUFFIXES.containsKey(lower)) suffix=NAME_SUFFIXES.get(lower);
            else nameTokens.add(token);
        }
        if (nameTokens.isEmpty()) return "INVALID_NAME: "+input;

        List<String> cased=new ArrayList<>();
        for (String token : nameTokens) cased.add(capitalizeNameToken(token));

        StringBuilder full=new StringBuilder();
        for (String tok : cased) full.append(tok).append(" ");
        String fullName=full.toString().trim();
        if (suffix!=null) fullName=fullName+" "+suffix;

        switch (mode) {
            case "first":
                return cased.isEmpty()?fullName:cased.get(0);
            case "last":
                return cased.isEmpty()?fullName:cased.get(cased.size()-1);
            case "initial_last": {
                // J. Jackson
                if (cased.size()<2) return "INVALID_NAME: "+input.trim()+" (need first and last)";
                String first=cased.get(0);
                String last=cased.get(cased.size()-1);
                String init=String.valueOf(Character.toUpperCase(first.charAt(0)))+".";
                return init+" "+last;
            }
            case "initialnodot_last": {
                // J.Jackson
                if (cased.size()<2) return "INVALID_NAME: "+input.trim()+" (need first and last)";
                String first=cased.get(0);
                String last=cased.get(cased.size()-1);
                String init=String.valueOf(Character.toUpperCase(first.charAt(0)));
                return init+"."+last;
            }
            case "full":
            default:
                if (cased.size()<2) return "INVALID_NAME: "+input.trim()+" (single name only)";
                return fullName;
        }
    }

    private String capitalizeNameToken(String token) {
        if (token==null||token.isEmpty()) return token;
        if (token.contains("-")) {
            String[] h=token.split("-");
            StringBuilder sb=new StringBuilder();
            for (int i=0;i<h.length;i++){sb.append(capitalizeNameToken(h[i]));if(i<h.length-1)sb.append("-");}
            return sb.toString();
        }
        if (token.matches("(?i)o'.*")&&token.length()>2) return "O'"+capitalizeFirst(token.substring(2));
        if (token.matches("(?i)mc.+")&&token.length()>2) return "Mc"+capitalizeFirst(token.substring(2));
        if (token.matches("(?i)mac[^aeiou].+")&&token.length()>3) return "Mac"+capitalizeFirst(token.substring(3));
        return capitalizeFirst(token);
    }

    private String capitalizeFirst(String s) {
        if (s==null||s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0))+s.substring(1).toLowerCase();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BOOLEAN NORMALIZATION
    // ─────────────────────────────────────────────────────────────────────────
    private String applyBool(String input, String[] parts) {
        if (input==null||input.trim().isEmpty()) return input;
        String trueOut="true"; String falseOut="false";
        if (parts.length>1) {
            String[] outputs=parts[1].split("/");
            if (outputs.length==2){trueOut=outputs[0];falseOut=outputs[1];}
        }
        Boolean boolValue=BOOL_MAP.get(input.trim().toLowerCase());
        if (boolValue==null) return "INVALID_BOOL: "+input;
        return boolValue?trueOut:falseOut;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ID VALIDATION
    // ─────────────────────────────────────────────────────────────────────────
    private String applyId(String input, String[] parts) {
        if (input==null||input.trim().isEmpty()) return input;
        String value=input.trim();
        String requiredStart=null,requiredEnd=null;
        for (int i=1;i<parts.length;i++) {
            String p=parts[i].toLowerCase();
            if (p.equals("startswith")&&i+1<parts.length) requiredStart=parts[++i];
            else if (p.equals("endswith")&&i+1<parts.length) requiredEnd=parts[++i];
        }
        if (requiredStart!=null) {
            String req=requiredStart.toLowerCase();
            if (req.equals("letter")){if(value.isEmpty()||!Character.isLetter(value.charAt(0))) return "INVALID_ID: "+input+" (expected to start with a letter)";}
            else if (req.equals("number")){if(value.isEmpty()||!Character.isDigit(value.charAt(0))) return "INVALID_ID: "+input+" (expected to start with a number)";}
            else{if(!value.toLowerCase().startsWith(requiredStart.toLowerCase())) return "INVALID_ID: "+input+" (expected to start with '"+requiredStart+"')";}
        }
        if (requiredEnd!=null) {
            String req=requiredEnd.toLowerCase();
            if (req.equals("letter")){if(value.isEmpty()||!Character.isLetter(value.charAt(value.length()-1))) return "INVALID_ID: "+input+" (expected to end with a letter)";}
            else if (req.equals("number")){if(value.isEmpty()||!Character.isDigit(value.charAt(value.length()-1))) return "INVALID_ID: "+input+" (expected to end with a number)";}
            else{if(!value.toLowerCase().endsWith(requiredEnd.toLowerCase())) return "INVALID_ID: "+input+" (expected to end with '"+requiredEnd+"')";}
        }
        return value;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VALIDATE: EMAIL / URL / ANY
    // ─────────────────────────────────────────────────────────────────────────
    private String applyValidate(String input, String[] parts) {
        String mode=(parts.length>1)?parts[1]:"any";
        switch (mode) {
            case "email": return validateEmail(input);
            case "url":   return validateUrl(input);
            case "any":
                String er=validateEmail(input);
                if (!er.startsWith("INVALID_EMAIL:")) return er;
                String ur=validateUrl(input);
                if (!ur.startsWith("INVALID_URL:")) return ur;
                return "INVALID_CONTACT: "+input;
            default: return input;
        }
    }

    private String validateEmail(String input) {
        if (input==null||input.trim().isEmpty()) return input;
        String cleaned=input.trim().replaceAll("\\s*@\\s*","@").replaceAll("\\s*\\.\\s*",".").toLowerCase();
        long atCount=cleaned.chars().filter(c->c=='@').count();
        if (atCount!=1) return "INVALID_EMAIL: "+input;
        int at=cleaned.indexOf('@');
        String local=cleaned.substring(0,at),domain=cleaned.substring(at+1);
        if (local.isEmpty()||domain.isEmpty()||!domain.contains(".")) return "INVALID_EMAIL: "+input;
        int lastDot=domain.lastIndexOf('.');
        if (domain.substring(0,lastDot).isEmpty()) return "INVALID_EMAIL: "+input;
        String tld=domain.substring(lastDot+1);
        if (tld.isEmpty()||!tld.matches("[a-z]+")) return "INVALID_EMAIL: "+input;
        return cleaned;
    }

    private String validateUrl(String input) {
        if (input==null||input.trim().isEmpty()) return input;
        String cleaned=input.trim().replaceAll("\\s+","").toLowerCase();
        String scheme="",rest=cleaned;
        if (cleaned.startsWith("https://")){scheme="https://";rest=cleaned.substring(8);}
        else if (cleaned.startsWith("http://")){scheme="http://";rest=cleaned.substring(7);}
        String host=rest.startsWith("www.")?rest.substring(4):rest;
        if (!host.contains(".")||host.indexOf('.')==0) return "INVALID_URL: "+input;
        String tld=host.substring(host.lastIndexOf('.')+1).split("[/?#]")[0];
        if (tld.isEmpty()||!tld.matches("[a-z0-9]+")) return "INVALID_URL: "+input;
        return scheme+(rest.startsWith("www.")?"www.":"")+host;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CASE NORMALIZATION
    // ─────────────────────────────────────────────────────────────────────────
    private String applyCase(String input,String[] parts) {
        if (parts.length<2) return input;
        switch (parts[1]) {
            case "upper":    return input.toUpperCase();
            case "lower":    return input.toLowerCase();
            case "title":    return toTitleCase(input);
            case "sentence": return toSentenceCase(input);
            default:         return input;
        }
    }
    private String toTitleCase(String input) {
        if (input==null||input.isEmpty()) return input;
        String[] words=input.toLowerCase().split(" ");
        StringBuilder sb=new StringBuilder();
        for (String word : words) if (!word.isEmpty()) sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        return sb.toString().trim();
    }
    private String toSentenceCase(String input) {
        if (input==null||input.isEmpty()) return input;
        String lower=input.toLowerCase().trim();
        return Character.toUpperCase(lower.charAt(0))+lower.substring(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CURRENCY CODE NORMALIZATION
    // ─────────────────────────────────────────────────────────────────────────
    private String applyCurrencyCode(String input,String[] parts) {
        if (input==null||input.trim().isEmpty()) return input;
        if (parts.length<2) return input;
        String upper=input.trim().toUpperCase();
        if (!KNOWN_CURRENCY_CODES.contains(upper)) return "INVALID_CURRENCY: "+input;
        switch (parts[1]) {
            case "upper":  return upper;
            case "lower":  return input.trim().toLowerCase();
            case "symbol": return CURRENCY_SYMBOLS.getOrDefault(upper,"INVALID_CURRENCY: "+input);
            default:       return input;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CURRENCY FORMAT
    // ─────────────────────────────────────────────────────────────────────────
    private String formatPercent(String input, String[] parts) {
        int places = 2;
        boolean useCommas = !contains(parts, "nocomma");
        for (int i = 1; i < parts.length; i++) {
            try { places = Integer.parseInt(parts[i]); break; }
            catch (NumberFormatException ignored) {}
        }
        String cleaned = input.trim().replace("%", "").trim();
        BigDecimal value = parseNumber(cleaned);
        if (value == null) return null;
        return formatWithDecimals(value, places, useCommas) + "%";
    }

    private String formatCurrency(String input,String[] parts) {
        String code=(parts.length>1)?parts[1].toUpperCase():"USD";
        boolean useCommas=!contains(parts,"nocomma");
        BigDecimal value=parseNumber(input);
        if (value==null) return null;
        String symbol=CURRENCY_SYMBOLS.getOrDefault(code,code+" ");
        return symbol+formatWithDecimals(value,2,useCommas);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DECIMAL
    // ─────────────────────────────────────────────────────────────────────────
    private String formatDecimal(String input,String[] parts) {
        int places=2; boolean useCommas=!contains(parts,"nocomma");
        for (int i=1;i<parts.length;i++){try{places=Integer.parseInt(parts[i]);break;}catch(NumberFormatException ignored){}}
        BigDecimal value=parseNumber(input);
        if (value==null) return null;
        return formatWithDecimals(value,places,useCommas);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ROUND
    // ─────────────────────────────────────────────────────────────────────────
    private String formatRound(String input,String[] parts) {
        String direction=(parts.length>1)?parts[1]:"nearest";
        boolean useCommas=!contains(parts,"nocomma");
        BigDecimal value=parseNumber(input);
        if (value==null) return null;
        BigDecimal rounded;
        switch(direction){case "up":rounded=value.setScale(0,RoundingMode.CEILING);break;case "down":rounded=value.setScale(0,RoundingMode.FLOOR);break;default:rounded=value.setScale(0,RoundingMode.HALF_UP);}
        return formatWholeNumber(rounded.longValue(),useCommas);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INTEGER
    // ─────────────────────────────────────────────────────────────────────────
    public String toInteger(String input) {
        if (input==null||input.trim().isEmpty()) return input;
        BigDecimal parsed=parseNumber(input);
        if (parsed!=null) return String.valueOf(parsed.longValue());
        try {
            String cleaned=input.trim().toLowerCase().replace("-"," ").replace(",","");
            String[] tokens=cleaned.split("\\s+");
            long result=0,current=0;boolean foundAny=false;
            for (String token : tokens) {
                if (!WORD_MAP.containsKey(token)) continue;
                foundAny=true; long val=WORD_MAP.get(token);
                if (val==100){current=current==0?100:current*100;}
                else if (val==1000||val==1000000){current=current==0?val:current*val;result+=current;current=0;}
                else{current+=val;}
            }
            result+=current;
            if (!foundAny) return null;
            if (result==0&&!input.toLowerCase().contains("zero")) return null;
            return String.valueOf(result);
        } catch(Exception e){return null;}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WORDS
    // ─────────────────────────────────────────────────────────────────────────
    public String toWords(String input) {
        if (input==null||input.trim().isEmpty()) return input;
        BigDecimal parsed=parseNumber(input);
        if (parsed==null) return "INVALID_NUMBER: "+input;
        try{return convertToWords(parsed.longValue());}catch(Exception e){return "INVALID_NUMBER: "+input;}
    }

    private String convertToWords(long number) {
        if (number==0) return "Zero";
        if (number<0) return "Negative "+convertToWords(-number);
        String[] ones={"","One","Two","Three","Four","Five","Six","Seven","Eight","Nine","Ten","Eleven","Twelve","Thirteen","Fourteen","Fifteen","Sixteen","Seventeen","Eighteen","Nineteen"};
        String[] tens={"","","Twenty","Thirty","Forty","Fifty","Sixty","Seventy","Eighty","Ninety"};
        if (number<20) return ones[(int)number];
        if (number<100){String t=tens[(int)number/10];String o=ones[(int)number%10];return o.isEmpty()?t:t+"-"+o;}
        if (number<1000){String r=convertToWords(number%100);String h=ones[(int)number/100]+" Hundred";return r.isEmpty()?h:h+" "+r;}
        if (number<1000000){String r=convertToWords(number%1000);String t=convertToWords(number/1000)+" Thousand";return r.isEmpty()?t:t+" "+r;}
        String r=convertToWords(number%1000000);String m=convertToWords(number/1000000)+" Million";return r.isEmpty()?m:m+" "+r;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NEGATIVE PARENS
    // ─────────────────────────────────────────────────────────────────────────
    private boolean isParensNotation(String input) {
        if (input==null) return false;
        String t=input.trim();
        return t.startsWith("(")&&t.endsWith(")");
    }
    private String parensToNegative(String input) {
        String inner=input.trim();
        inner=inner.substring(1,inner.length()-1).trim();
        return "-"+inner;
    }
    private String applyParensOutput(String result) {
        if (result==null) return null;
        String t=result.trim();
        if (t.startsWith("(")&&t.endsWith(")")) return result;
        if (t.startsWith("-")) return "("+t.substring(1)+")";
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private BigDecimal parseNumber(String input) {
        if (input==null) return null;
        String cleaned=input.trim().replaceAll("^\"+|\"+$","").replaceAll("(?i)^(INVALID_NUMBER:|CAP_EXCEEDED:)\\s*","");
        if (cleaned.startsWith("(")&&cleaned.endsWith(")"))
            cleaned="-"+cleaned.substring(1,cleaned.length()-1).trim();
        List<String> symbols=new ArrayList<>(CURRENCY_SYMBOLS.values());
        symbols.sort((a,b)->b.length()-a.length());
        for (String sym : symbols) cleaned=cleaned.replace(sym.trim(),"");
        for (String code : CURRENCY_SYMBOLS.keySet()) cleaned=cleaned.replaceAll("(?i)\\b"+code+"\\b","");
        cleaned=cleaned.trim();
        if (!allowScientificNotation&&cleaned.matches(".*[eE][+\\-]?\\d+.*")) return null;
        boolean hasComma=cleaned.contains(","),hasPeriod=cleaned.contains(".");
        if (hasComma&&hasPeriod){if(cleaned.lastIndexOf(",")>cleaned.lastIndexOf("."))cleaned=cleaned.replace(".","").replace(",",".");else cleaned=cleaned.replace(",","");}
        else if (hasComma){String ac=cleaned.substring(cleaned.lastIndexOf(",")+1);cleaned=ac.length()==3?cleaned.replace(",",""):cleaned.replace(",",".");}
        cleaned=cleaned.replace("'","").replaceAll("\\s+","").trim();
        if (cleaned.isEmpty()) return null;
        try{return new BigDecimal(cleaned);}catch(NumberFormatException e){return null;}
    }

    private String formatWithDecimals(BigDecimal value,int places,boolean useCommas) {
        BigDecimal scaled=value.setScale(places,RoundingMode.HALF_UP);
        if (useCommas) return new DecimalFormat(buildPattern(places),DecimalFormatSymbols.getInstance(Locale.US)).format(scaled);
        return places>0?String.format("%."+places+"f",scaled):String.valueOf(scaled.longValue());
    }

    private String formatWholeNumber(long value,boolean useCommas) {
        if (useCommas) return new DecimalFormat("#,##0",DecimalFormatSymbols.getInstance(Locale.US)).format(value);
        return String.valueOf(value);
    }

    private String[] toLower(String[] parts) {
        String[] lower=new String[parts.length];
        for (int i=0;i<parts.length;i++) lower[i]=parts[i]!=null?parts[i].toLowerCase():"";
        return lower;
    }

    private boolean contains(String[] parts,String token) {
        for (String p : parts) if (p.equalsIgnoreCase(token)) return true;
        return false;
    }
}