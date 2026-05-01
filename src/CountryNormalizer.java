import java.util.*;

public class CountryNormalizer {

    private static final Map<String, String> toIso2     = new HashMap<>();
    private static final Map<String, String> toIso3     = new HashMap<>();
    private static final Map<String, String> toFullName = new HashMap<>();

    // Every variation registered directly to all three output formats at once.
    // No middleman. Any input → any output.
    private static void add(String iso2, String iso3, String fullName, String... variations) {
        for (String v : variations) {
            String key = v.toLowerCase().trim();
            toIso2.put(key, iso2);
            toIso3.put(key, iso3);
            toFullName.put(key, fullName);
        }
    }

    static {
        add("AF", "AFG", "Afghanistan",
                "af", "afg", "afghanistan");

        add("AL", "ALB", "Albania",
                "al", "alb", "albania");

        add("DZ", "DZA", "Algeria",
                "dz", "dza", "algeria");

        add("AD", "AND", "Andorra",
                "ad", "and", "andorra");

        add("AO", "AGO", "Angola",
                "ao", "ago", "angola");

        add("AG", "ATG", "Antigua and Barbuda",
                "ag", "atg", "antigua", "barbuda", "antigua and barbuda");

        add("AR", "ARG", "Argentina",
                "ar", "arg", "argentina");

        add("AM", "ARM", "Armenia",
                "am", "arm", "armenia");

        add("AU", "AUS", "Australia",
                "au", "aus", "australia", "oz");

        add("AT", "AUT", "Austria",
                "at", "aut", "austria");

        add("AZ", "AZE", "Azerbaijan",
                "az", "aze", "azerbaijan");

        add("BS", "BHS", "Bahamas",
                "bs", "bhs", "bahamas", "the bahamas");

        add("BH", "BHR", "Bahrain",
                "bh", "bhr", "bahrain");

        add("BD", "BGD", "Bangladesh",
                "bd", "bgd", "bangladesh");

        add("BB", "BRB", "Barbados",
                "bb", "brb", "barbados");

        add("BY", "BLR", "Belarus",
                "by", "blr", "belarus");

        add("BE", "BEL", "Belgium",
                "be", "bel", "belgium");

        add("BZ", "BLZ", "Belize",
                "bz", "blz", "belize");

        add("BJ", "BEN", "Benin",
                "bj", "ben", "benin");

        add("BT", "BTN", "Bhutan",
                "bt", "btn", "bhutan");

        add("BO", "BOL", "Bolivia",
                "bo", "bol", "bolivia");

        add("BA", "BIH", "Bosnia and Herzegovina",
                "ba", "bih", "bosnia", "herzegovina", "bosnia and herzegovina");

        add("BW", "BWA", "Botswana",
                "bw", "bwa", "botswana");

        add("BR", "BRA", "Brazil",
                "br", "bra", "brazil", "brasil");

        add("BN", "BRN", "Brunei",
                "bn", "brn", "brunei", "brunei darussalam");

        add("BG", "BGR", "Bulgaria",
                "bg", "bgr", "bulgaria");

        add("BF", "BFA", "Burkina Faso",
                "bf", "bfa", "burkina faso", "burkina");

        add("BI", "BDI", "Burundi",
                "bi", "bdi", "burundi");

        add("CV", "CPV", "Cape Verde",
                "cv", "cpv", "cape verde", "cabo verde");

        add("KH", "KHM", "Cambodia",
                "kh", "khm", "cambodia", "kampuchea");

        add("CM", "CMR", "Cameroon",
                "cm", "cmr", "cameroon");

        add("CA", "CAN", "Canada",
                "ca", "can", "canada");

        add("CF", "CAF", "Central African Republic",
                "cf", "caf", "central african republic", "car");

        add("TD", "TCD", "Chad",
                "td", "tcd", "chad");

        add("CL", "CHL", "Chile",
                "cl", "chl", "chile");

        add("CN", "CHN", "China",
                "cn", "chn", "china", "prc",
                "peoples republic of china", "people's republic of china", "mainland china");

        add("CO", "COL", "Colombia",
                "co", "col", "colombia");

        add("KM", "COM", "Comoros",
                "km", "com", "comoros");

        add("CG", "COG", "Congo",
                "cg", "cog", "congo", "republic of congo");

        add("CD", "COD", "Democratic Republic of the Congo",
                "cd", "cod", "dr congo", "drc",
                "democratic republic of congo",
                "democratic republic of the congo", "zaire");

        add("CR", "CRI", "Costa Rica",
                "cr", "cri", "costa rica");

        add("HR", "HRV", "Croatia",
                "hr", "hrv", "croatia");

        add("CU", "CUB", "Cuba",
                "cu", "cub", "cuba");

        add("CY", "CYP", "Cyprus",
                "cy", "cyp", "cyprus");

        add("CZ", "CZE", "Czech Republic",
                "cz", "cze", "czech republic", "czechia", "czech");

        add("DK", "DNK", "Denmark",
                "dk", "dnk", "denmark");

        add("DJ", "DJI", "Djibouti",
                "dj", "dji", "djibouti");

        add("DM", "DMA", "Dominica",
                "dm", "dma", "dominica");

        add("DO", "DOM", "Dominican Republic",
                "do", "dom", "dominican republic");

        add("EC", "ECU", "Ecuador",
                "ec", "ecu", "ecuador");

        add("EG", "EGY", "Egypt",
                "eg", "egy", "egypt");

        add("SV", "SLV", "El Salvador",
                "sv", "slv", "el salvador", "salvador");

        add("GQ", "GNQ", "Equatorial Guinea",
                "gq", "gnq", "equatorial guinea");

        add("ER", "ERI", "Eritrea",
                "er", "eri", "eritrea");

        add("EE", "EST", "Estonia",
                "ee", "est", "estonia");

        add("SZ", "SWZ", "Eswatini",
                "sz", "swz", "eswatini", "swaziland");

        add("ET", "ETH", "Ethiopia",
                "et", "eth", "ethiopia");

        add("FJ", "FJI", "Fiji",
                "fj", "fji", "fiji");

        add("FI", "FIN", "Finland",
                "fi", "fin", "finland");

        add("FR", "FRA", "France",
                "fr", "fra", "france");

        add("GA", "GAB", "Gabon",
                "ga", "gab", "gabon");

        add("GM", "GMB", "Gambia",
                "gm", "gmb", "gambia", "the gambia");

        add("GE", "GEO", "Georgia",
                "ge", "geo", "georgia");

        add("DE", "DEU", "Germany",
                "de", "deu", "germany", "deutschland");

        add("GH", "GHA", "Ghana",
                "gh", "gha", "ghana");

        add("GR", "GRC", "Greece",
                "gr", "grc", "greece", "hellas");

        add("GD", "GRD", "Grenada",
                "gd", "grd", "grenada");

        add("GT", "GTM", "Guatemala",
                "gt", "gtm", "guatemala");

        add("GN", "GIN", "Guinea",
                "gn", "gin", "guinea");

        add("GW", "GNB", "Guinea-Bissau",
                "gw", "gnb", "guinea-bissau", "guinea bissau");

        add("GY", "GUY", "Guyana",
                "gy", "guy", "guyana");

        add("HT", "HTI", "Haiti",
                "ht", "hti", "haiti");

        add("HN", "HND", "Honduras",
                "hn", "hnd", "honduras");

        add("HU", "HUN", "Hungary",
                "hu", "hun", "hungary");

        add("IS", "ISL", "Iceland",
                "is", "isl", "iceland");

        add("IN", "IND", "India",
                "in", "ind", "india");

        add("ID", "IDN", "Indonesia",
                "id", "idn", "indonesia");

        add("IR", "IRN", "Iran",
                "ir", "irn", "iran", "islamic republic of iran");

        add("IQ", "IRQ", "Iraq",
                "iq", "irq", "iraq");

        add("IE", "IRL", "Ireland",
                "ie", "irl", "ireland", "republic of ireland");

        add("IL", "ISR", "Israel",
                "il", "isr", "israel");

        add("IT", "ITA", "Italy",
                "it", "ita", "italy", "italia");

        add("JM", "JAM", "Jamaica",
                "jm", "jam", "jamaica");

        add("JP", "JPN", "Japan",
                "jp", "jpn", "japan");

        add("JO", "JOR", "Jordan",
                "jo", "jor", "jordan");

        add("KZ", "KAZ", "Kazakhstan",
                "kz", "kaz", "kazakhstan");

        add("KE", "KEN", "Kenya",
                "ke", "ken", "kenya");

        add("KI", "KIR", "Kiribati",
                "ki", "kir", "kiribati");

        add("KP", "PRK", "North Korea",
                "kp", "prk", "north korea", "dprk");

        add("KR", "KOR", "South Korea",
                "kr", "kor", "south korea", "korea", "republic of korea");

        add("KW", "KWT", "Kuwait",
                "kw", "kwt", "kuwait");

        add("KG", "KGZ", "Kyrgyzstan",
                "kg", "kgz", "kyrgyzstan", "kyrgyz republic");

        add("LA", "LAO", "Laos",
                "la", "lao", "laos");

        add("LV", "LVA", "Latvia",
                "lv", "lva", "latvia");

        add("LB", "LBN", "Lebanon",
                "lb", "lbn", "lebanon");

        add("LS", "LSO", "Lesotho",
                "ls", "lso", "lesotho");

        add("LR", "LBR", "Liberia",
                "lr", "lbr", "liberia");

        add("LY", "LBY", "Libya",
                "ly", "lby", "libya");

        add("LI", "LIE", "Liechtenstein",
                "li", "lie", "liechtenstein");

        add("LT", "LTU", "Lithuania",
                "lt", "ltu", "lithuania");

        add("LU", "LUX", "Luxembourg",
                "lu", "lux", "luxembourg");

        add("MG", "MDG", "Madagascar",
                "mg", "mdg", "madagascar");

        add("MW", "MWI", "Malawi",
                "mw", "mwi", "malawi");

        add("MY", "MYS", "Malaysia",
                "my", "mys", "malaysia");

        add("MV", "MDV", "Maldives",
                "mv", "mdv", "maldives");

        add("ML", "MLI", "Mali",
                "ml", "mli", "mali");

        add("MT", "MLT", "Malta",
                "mt", "mlt", "malta");

        add("MH", "MHL", "Marshall Islands",
                "mh", "mhl", "marshall islands");

        add("MR", "MRT", "Mauritania",
                "mr", "mrt", "mauritania");

        add("MU", "MUS", "Mauritius",
                "mu", "mus", "mauritius");

        add("MX", "MEX", "Mexico",
                "mx", "mex", "mexico", "méxico");

        add("FM", "FSM", "Micronesia",
                "fm", "fsm", "micronesia");

        add("MD", "MDA", "Moldova",
                "md", "mda", "moldova");

        add("MC", "MCO", "Monaco",
                "mc", "mco", "monaco");

        add("MN", "MNG", "Mongolia",
                "mn", "mng", "mongolia");

        add("ME", "MNE", "Montenegro",
                "me", "mne", "montenegro");

        add("MA", "MAR", "Morocco",
                "ma", "mar", "morocco");

        add("MZ", "MOZ", "Mozambique",
                "mz", "moz", "mozambique");

        add("MM", "MMR", "Myanmar",
                "mm", "mmr", "myanmar", "burma");

        add("NA", "NAM", "Namibia",
                "na", "nam", "namibia");

        add("NR", "NRU", "Nauru",
                "nr", "nru", "nauru");

        add("NP", "NPL", "Nepal",
                "np", "npl", "nepal");

        add("NL", "NLD", "Netherlands",
                "nl", "nld", "netherlands", "holland", "the netherlands");

        add("NZ", "NZL", "New Zealand",
                "nz", "nzl", "new zealand");

        add("NI", "NIC", "Nicaragua",
                "ni", "nic", "nicaragua");

        add("NE", "NER", "Niger",
                "ne", "ner", "niger");

        add("NG", "NGA", "Nigeria",
                "ng", "nga", "nigeria");

        add("MK", "MKD", "North Macedonia",
                "mk", "mkd", "north macedonia", "macedonia");

        add("NO", "NOR", "Norway",
                "no", "nor", "norway");

        add("OM", "OMN", "Oman",
                "om", "omn", "oman");

        add("PK", "PAK", "Pakistan",
                "pk", "pak", "pakistan");

        add("PW", "PLW", "Palau",
                "pw", "plw", "palau");

        add("PA", "PAN", "Panama",
                "pa", "pan", "panama");

        add("PG", "PNG", "Papua New Guinea",
                "pg", "png", "papua new guinea");

        add("PY", "PRY", "Paraguay",
                "py", "pry", "paraguay");

        add("PE", "PER", "Peru",
                "pe", "per", "peru");

        add("PH", "PHL", "Philippines",
                "ph", "phl", "philippines", "the philippines");

        add("PL", "POL", "Poland",
                "pl", "pol", "poland");

        add("PT", "PRT", "Portugal",
                "pt", "prt", "portugal");

        add("QA", "QAT", "Qatar",
                "qa", "qat", "qatar");

        add("RO", "ROU", "Romania",
                "ro", "rou", "romania");

        add("RU", "RUS", "Russia",
                "ru", "rus", "russia", "russian federation");

        add("RW", "RWA", "Rwanda",
                "rw", "rwa", "rwanda");

        add("KN", "KNA", "Saint Kitts and Nevis",
                "kn", "kna", "saint kitts", "st kitts", "saint kitts and nevis");

        add("LC", "LCA", "Saint Lucia",
                "lc", "lca", "saint lucia", "st lucia");

        add("VC", "VCT", "Saint Vincent and the Grenadines",
                "vc", "vct", "saint vincent", "st vincent",
                "saint vincent and the grenadines");

        add("WS", "WSM", "Samoa",
                "ws", "wsm", "samoa");

        add("SM", "SMR", "San Marino",
                "sm", "smr", "san marino");

        add("ST", "STP", "Sao Tome and Principe",
                "st", "stp", "sao tome", "sao tome and principe");

        add("SA", "SAU", "Saudi Arabia",
                "sa", "sau", "saudi arabia", "saudi", "ksa");

        add("SN", "SEN", "Senegal",
                "sn", "sen", "senegal");

        add("RS", "SRB", "Serbia",
                "rs", "srb", "serbia");

        add("SC", "SYC", "Seychelles",
                "sc", "syc", "seychelles");

        add("SL", "SLE", "Sierra Leone",
                "sl", "sle", "sierra leone");

        add("SG", "SGP", "Singapore",
                "sg", "sgp", "singapore");

        add("SK", "SVK", "Slovakia",
                "sk", "svk", "slovakia");

        add("SI", "SVN", "Slovenia",
                "si", "svn", "slovenia");

        add("SB", "SLB", "Solomon Islands",
                "sb", "slb", "solomon islands");

        add("SO", "SOM", "Somalia",
                "so", "som", "somalia");

        add("ZA", "ZAF", "South Africa",
                "za", "zaf", "south africa", "rsa");

        add("SS", "SSD", "South Sudan",
                "ss", "ssd", "south sudan");

        add("ES", "ESP", "Spain",
                "es", "esp", "spain", "espana", "españa");

        add("LK", "LKA", "Sri Lanka",
                "lk", "lka", "sri lanka", "ceylon");

        add("SD", "SDN", "Sudan",
                "sd", "sdn", "sudan");

        add("SR", "SUR", "Suriname",
                "sr", "sur", "suriname", "surinam");

        add("SE", "SWE", "Sweden",
                "se", "swe", "sweden");

        add("CH", "CHE", "Switzerland",
                "ch", "che", "switzerland", "swiss");

        add("SY", "SYR", "Syria",
                "sy", "syr", "syria");

        add("TW", "TWN", "Taiwan",
                "tw", "twn", "taiwan");

        add("TJ", "TJK", "Tajikistan",
                "tj", "tjk", "tajikistan");

        add("TZ", "TZA", "Tanzania",
                "tz", "tza", "tanzania");

        add("TH", "THA", "Thailand",
                "th", "tha", "thailand");

        add("TL", "TLS", "Timor-Leste",
                "tl", "tls", "timor-leste", "east timor", "timor leste");

        add("TG", "TGO", "Togo",
                "tg", "tgo", "togo");

        add("TO", "TON", "Tonga",
                "to", "ton", "tonga");

        add("TT", "TTO", "Trinidad and Tobago",
                "tt", "tto", "trinidad", "tobago", "trinidad and tobago");

        add("TN", "TUN", "Tunisia",
                "tn", "tun", "tunisia");

        add("TR", "TUR", "Turkey",
                "tr", "tur", "turkey", "turkiye", "türkiye");

        add("TM", "TKM", "Turkmenistan",
                "tm", "tkm", "turkmenistan");

        add("TV", "TUV", "Tuvalu",
                "tv", "tuv", "tuvalu");

        add("UG", "UGA", "Uganda",
                "ug", "uga", "uganda");

        add("UA", "UKR", "Ukraine",
                "ua", "ukr", "ukraine");

        add("AE", "ARE", "United Arab Emirates",
                "ae", "are", "uae", "united arab emirates", "emirates");

        add("GB", "GBR", "United Kingdom",
                "gb", "gbr", "uk", "u.k", "u.k.", "g.b", "great britain",
                "united kingdom", "britain", "england", "scotland", "wales");

        add("US", "USA", "United States",
                "us", "usa", "u.s", "u.s.a", "u.s.a.", "united states",
                "united states of america", "the united states", "america");

        add("UY", "URY", "Uruguay",
                "uy", "ury", "uruguay");

        add("UZ", "UZB", "Uzbekistan",
                "uz", "uzb", "uzbekistan");

        add("VU", "VUT", "Vanuatu",
                "vu", "vut", "vanuatu");

        add("VE", "VEN", "Venezuela",
                "ve", "ven", "venezuela");

        add("VN", "VNM", "Vietnam",
                "vn", "vnm", "vietnam", "viet nam");

        add("YE", "YEM", "Yemen",
                "ye", "yem", "yemen");

        add("ZM", "ZMB", "Zambia",
                "zm", "zmb", "zambia");

        add("ZW", "ZWE", "Zimbabwe",
                "zw", "zwe", "zimbabwe");
    }

    // ── Public normalize method ───────────────────────────────────────────────

    public String normalize(String input, String format) {
        if (input == null || input.trim().isEmpty()) return input;

        String key = input.toLowerCase().trim();

        switch (format.toLowerCase()) {
            case "iso2":     return toIso2.getOrDefault(key, input);
            case "iso3":     return toIso3.getOrDefault(key, input);
            case "fullname": return toFullName.getOrDefault(key, input);
            default:         return input;
        }
    }
}