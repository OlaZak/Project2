import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AmountInWords {
    public static class Currency {
        protected int code;
        protected String name;

        protected String oneInteger, twoIntegers, fiveIntegers;
        protected Sex integerSex;

        protected String oneFraction, twoFractions, fiveFractions;
        protected Sex fractionSex;

        public static Currency byCode(int code) {
            for (Currency c : currencies)
                if (c.getCode() == code)
                    return c;
            return null;
        }

        public static Currency byCode(String code) {
            return byCode(Integer.parseInt(code));
        }

        public static Currency byName(String name) {
            for (Currency c : currencies)
                if (c.getName().equals(name))
                    return c;
            return null;
        }

        public Currency(int code, String name) {
            this.code = code;
            this.name = name;
        }

        public int getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public boolean equals(Object o) {
            return (o instanceof Currency) && code == ((Currency) o).code;
        }

        public int hashCode() {
            return code;
        }

        public String toString() {
            return code + ":" + name;
        }
    }

    public static enum Sex {MALE, FEMALE}

    public static interface CurrencyMapping<T> {
        Currency getCurrency(T currency);
    }

    private static final class DefaultCurrencyMapping implements CurrencyMapping<Currency> {
        public Currency getCurrency(Currency currency) {
            return currency;
        }
    }

    public static final Currency UAH = new Currency(980, "UAH") {{
        oneInteger = ONE_UAH_INTEGER;
        twoIntegers = TWO_UAH_INTEGER;
        fiveIntegers = FIVE_UAH_INTEGER;
        integerSex = Sex.FEMALE;

        oneFraction = ONE_UAH_FRACTION;
        twoFractions = TWO_UAH_FRACTION;
        fiveFractions = FIVE_UAH_FRACTION;
        fractionSex = Sex.FEMALE;
    }};

    public static final Currency USD = new Currency(840, "USD") {{
        oneInteger = ONE_USD_INTEGER;
        twoIntegers = TWO_USD_INTEGER;
        oneFraction = ONE_USD_FRACTION;
        twoFractions = TWO_USD_FRACTION;
    }};

    private static final List<Currency> currencies = new CopyOnWriteArrayList<Currency>() {{
        add(UAH);
        add(USD);
    }};

    public static void addCurrency(Currency currency) {
        if (currency == null) throw new NullPointerException("Currency is null");

        if (currency.oneInteger == null
                || currency.twoIntegers == null
                || currency.fiveIntegers == null
                || currency.integerSex == null
                || currency.oneFraction == null
                || currency.twoFractions == null
                || currency.fiveFractions == null
                || currency.fractionSex == null)
            throw new NullPointerException("Currency " + currency + " is not properly initialized");

        for (Currency c : currencies)
            if (c.getCode() == currency.getCode() || c.getName().equals(currency.getName()))
                throw new IllegalStateException("Currency " + currency + "already registered");

        Currency copy = new Currency(currency.getCode(), currency.getName());
        copy.oneInteger = currency.oneInteger;
        copy.twoIntegers = currency.twoIntegers;
        copy.fiveIntegers = currency.fiveIntegers;
        copy.integerSex = currency.integerSex;
        copy.oneFraction = currency.oneFraction;
        copy.twoFractions = currency.twoFractions;
        copy.fiveFractions = currency.fiveFractions;
        copy.fractionSex = currency.fractionSex;

        currencies.add(copy);
    }

    public static void removeCurrency(Currency currency) {
        currencies.remove(currency);
    }

    public static List<Currency> getCurrencies() {
        return new ArrayList<Currency>(currencies);
    }

    private static volatile CurrencyMapping<?> currencyMapping = new DefaultCurrencyMapping();

    public static CurrencyMapping getCurrencyMapping() {
        return currencyMapping;
    }

    public static void setCurrencyMapping(CurrencyMapping mapping) {
        if (mapping == null) currencyMapping = new DefaultCurrencyMapping();
        else currencyMapping = mapping;
    }

    public static <T> String format(long amount, T currency, String language) {
        if (amount > 214748364700L || amount < 000L)
            throw new UnsupportedOperationException("Amounts grater than 2147483647.00 are not supported.");

        @SuppressWarnings({"unchecked"})
        Currency c = getCurrencyMapping().getCurrency(currency);

        if (c == null)
            throw new IllegalArgumentException("Currency " + currency + " is not found");
Long amount1 = amount;
        boolean notEmpty = (amount / 100 / 1000) == 0;

        Triad integerUnits = new Triad(c.oneInteger, c.twoIntegers, c.fiveIntegers, c.integerSex, notEmpty);
        Triad fractionUnits = new Triad(c.oneFraction, c.twoFractions, c.fiveFractions, c.fractionSex, true) {

            @Override
            int getTriadFromAmount(long amount) {
                return (int) (amount % 100);
            }
        };

        TriadENG integerUnitsENG = new TriadENG(c.oneInteger,c.twoIntegers, notEmpty);
        TriadENG fractionUnitsENG = new TriadENG(c.oneFraction,c.twoFractions, true){

            @Override
            int getTriadENGFromAmount(long amount) {
                return (int) (amount % 100);
            }
        };

        List<Triad> triads = Arrays.asList(Triad.BILLION, Triad.MILLION, Triad.THOUSAND,
                integerUnits, fractionUnits);

        List<TriadENG> triadENGS = Arrays.asList(TriadENG.BILLION_ENG, TriadENG.MILLION_ENG, TriadENG.THOUSAND_ENG,
                integerUnitsENG, fractionUnitsENG);

        StringBuilder amountInWords = new StringBuilder();

        if (language.contains("UA")) {
            for (Triad triad : triads)
                amountInWords.append(triadToWord(triad, triad.getTriadFromAmount(amount)));
        } else if (language.contains("ENG")) {
            for (TriadENG triadENG : triadENGS)
                amountInWords.append(triadToWordENG(triadENG, triadENG.getTriadENGFromAmount(amount)));
        }
        return amountInWords.toString();
    }


    private static String triadToWord(Triad triad, int value) {

        StringBuilder builder = new StringBuilder();

        if (value == 0) {
            if (!triad.mandatory) return "";

            if (triad.zero) return WORD_0 + " " + ending(triad, value);
            else return ending(triad, value);
        }

        int hundreds = value / 100;
        int tens = (value % 100) / 10;
        int units = value % 10;

        fromNum100ToWord(builder, hundreds,WORD_100,WORD_200,WORD_300,WORD_400,WORD_500,WORD_600,WORD_700,WORD_800,WORD_900);

        if (hundreds > 0) builder.append(' ');

        fromNum100ToWord(builder,tens,"",WORD_20,WORD_30,WORD_40,WORD_50,WORD_60,WORD_70,WORD_80,WORD_90);

        if (tens == 1) { fromNumToWord(builder, units, WORD_10,WORD_11,WORD_12,WORD_13,WORD_14,WORD_15,WORD_16,WORD_17,WORD_18,WORD_19); }
        if (tens > 0) builder.append(' ');

        if (tens != 1) {
            fromNum100ToWord(builder,units,triad.sex.equals(Sex.MALE) ? WORD_1_MALE : WORD_1_FEMALE,triad.sex.equals(Sex.MALE) ? WORD_2_MALE : WORD_2_FEMALE,
                    WORD_3,WORD_4,WORD_5,WORD_6,WORD_7,WORD_8,WORD_9);
            if (units > 0) builder.append(' ');
        }

        builder.append(ending(triad, value));

        return builder.toString();
    }
    private static String triadToWordENG(TriadENG triadENG, int value) {

        StringBuilder builder = new StringBuilder();

        if (value == 0) {
            if (!triadENG.mandatory) return "";

            if (triadENG.zero) return WORD_ENG_0 + " " + endingENG(triadENG, value);
            else return endingENG(triadENG, value);
        }

        int hundreds = value / 100;
        int tens = (value % 100) / 10;
        int units = value % 10;

        fromNum100ToWord(builder, hundreds,WORD_ENG_100,WORD_ENG_200,WORD_ENG_300,WORD_ENG_400,WORD_ENG_500,WORD_ENG_600,WORD_ENG_700,WORD_ENG_800,WORD_ENG_900);

        if (hundreds > 0) builder.append(' ');

        fromNum100ToWord(builder,tens,"",WORD_ENG_20,WORD_ENG_30,WORD_ENG_40,WORD_ENG_50,WORD_ENG_60,WORD_ENG_70,WORD_ENG_80,WORD_ENG_90);

        if (tens == 1) { fromNumToWord(builder, units, WORD_ENG_10,WORD_ENG_11,WORD_ENG_12,WORD_ENG_13,WORD_ENG_14,WORD_ENG_15,WORD_ENG_16,WORD_ENG_17,WORD_ENG_18,WORD_ENG_19); }
        if (tens > 0) builder.append(' ');

        if (tens != 1) {
            fromNum100ToWord(builder,units,WORD_ENG_1,WORD_ENG_2, WORD_ENG_3,WORD_ENG_4,WORD_ENG_5,WORD_ENG_6,WORD_ENG_7,WORD_ENG_8,WORD_ENG_9);
            if (units > 0) builder.append(' ');
        }

        builder.append(endingENG(triadENG, value));

        return builder.toString();
    }


    private static String ending(Triad triad, int value) {
        int tens = (value % 100) / 10;
        int units = value % 10;

        if (tens == 1) return triad.five + " ";

        String ending;
        switch (units) {
            default:
                ending = triad.five;
                break;
            case 1:
                ending = triad.one;
                break;
            case 2:
            case 3:
            case 4:
                ending = triad.two;
                break;
        }
        return ending + " ";
    }
    private static String endingENG(TriadENG triadENG, int value) {
        int tens = (value % 100) / 10;
        int units = value % 10;

        if (tens == 1) return triadENG.two + " ";

        String ending;
        switch (units) {
            default:
                ending = triadENG.two;
                break;
            case 1:
                ending = triadENG.one;
                break;
        }
        return ending + " ";
    }


    private static class Triad {
        static final Triad THOUSAND = new Triad(THOUSAND_ONE, THOUSAND_TWO, THOUSAND_FIVE, Sex.FEMALE, 3);
        static final Triad MILLION = new Triad(MILLION_ONE, MILLION_TWO, MILLION_FIVE, Sex.MALE, 6);
        static final Triad BILLION = new Triad(BILLION_ONE, BILLION_TWO, BILLION_FIVE, Sex.MALE, 9);

        private Triad(String one, String two, String five, Sex sex, long divisor) {
            this(one, two, five, sex, false, false, divisor);
        }

        private Triad(String one, String two, String five, Sex sex, boolean zero) {
            this(one, two, five, sex, true, zero, 0);
        }

        private Triad(String one, String two, String five, Sex sex, boolean mandatory, boolean zero, long power) {
            this.one = one;
            this.two = two;
            this.five = five;
            this.sex = sex;
            this.mandatory = mandatory;
            this.zero = zero;
            this.power = power;
        }

        String one;
        String two;
        String five;
        Sex sex;
        boolean mandatory;
        boolean zero;
        long power = 0;

        int getTriadFromAmount(long amount) {
            long divisor = (long) Math.pow(10, power + 2);
            return (int) (amount / divisor % 1000);
        }
    }
    private static class TriadENG {
        static final TriadENG THOUSAND_ENG = new TriadENG(THOUSAND_ENG_ONE, THOUSAND_ENG_TWO, 3);
        static final TriadENG MILLION_ENG = new TriadENG(MILLION_ENG_ONE, MILLION_ENG_TWO, 6);
        static final TriadENG BILLION_ENG = new TriadENG(BILLION_ENG_ONE, BILLION_ENG_TWO, 9);

        private TriadENG(String one, String two, long divisor) {
            this(one, two, false, false, divisor);
        }

        private TriadENG(String one, String two, boolean zero) {
            this(one, two,true, zero, 0);
        }

        private TriadENG(String one,String two,  boolean mandatory, boolean zero, long power) {
            this.one = one;
            this.two = two;
            this.mandatory = mandatory;
            this.zero = zero;
            this.power = power;
        }

        String one;
        String two;
        boolean mandatory;
        boolean zero;
        long power = 0;

        int getTriadENGFromAmount(long amount) {
            long divisor = (long) Math.pow(10, power + 2);
            return (int) (amount / divisor % 1000);
        }
    }

    private static void fromNum100ToWord(StringBuilder builder, int hundreds,String one,String two,String three,String four,String five,String six,String seven,String eight,String nine) {
        switch (hundreds) {
            default: break;
            case 1:  builder.append(one); break;
            case 2:  builder.append(two); break;
            case 3:  builder.append(three); break;
            case 4:  builder.append(four); break;
            case 5:  builder.append(five); break;
            case 6:  builder.append(six); break;
            case 7:  builder.append(seven); break;
            case 8:  builder.append(eight); break;
            case 9:  builder.append(nine); break;
        }
    }
    private static void fromNumToWord(StringBuilder builder, int units, String zero, String one,String two,String three,String four,String five,String six,String seven,String eight,String nine) {
        switch (units) {
            case 0: builder.append(zero); break;
            case 1: builder.append(one); break;
            case 2: builder.append(two); break;
            case 3: builder.append(three); break;
            case 4: builder.append(four); break;
            case 5: builder.append(five); break;
            case 6: builder.append(six); break;
            case 7: builder.append(seven); break;
            case 8: builder.append(eight); break;
            case 9: builder.append(nine); break;
        }
    }


    private AmountInWords() {
    }
    private static final String ONE_UAH_INTEGER = "гривня";
    private static final String TWO_UAH_INTEGER = "гривні";
    private static final String FIVE_UAH_INTEGER = "гривень";
    private static final String ONE_UAH_FRACTION = "копійка";
    private static final String TWO_UAH_FRACTION = "копійки";
    private static final String FIVE_UAH_FRACTION = "копійок";

    private static final String ONE_USD_INTEGER = "dollar";
    private static final String TWO_USD_INTEGER = "dollars";
    private static final String ONE_USD_FRACTION = "cent";
    private static final String TWO_USD_FRACTION = "cents";


    private static final String WORD_100 = "сто";
    private static final String WORD_200 = "двіст";
    private static final String WORD_300 = "триста";
    private static final String WORD_400 = "чотириста";
    private static final String WORD_500 = "п'ятсот";
    private static final String WORD_600 = "шістьсот";
    private static final String WORD_700 = "сімсот";
    private static final String WORD_800 = "вісімсот";
    private static final String WORD_900 = "дев'ятьсот";

    private static final String WORD_ENG_100 = "one hundred";
    private static final String WORD_ENG_200 = "two hundred";
    private static final String WORD_ENG_300 = "three hundred";
    private static final String WORD_ENG_400 = "four hundred";
    private static final String WORD_ENG_500 = "five hundred";
    private static final String WORD_ENG_600 = "six hundred";
    private static final String WORD_ENG_700 = "seven hundred";
    private static final String WORD_ENG_800 = "eight hundred";
    private static final String WORD_ENG_900 = "nine hundred";

    private static final String WORD_20 = "двадцать";
    private static final String WORD_30 = "тридцать";
    private static final String WORD_40 = "сорок";
    private static final String WORD_50 = "п'ятьдесят";
    private static final String WORD_60 = "шістьдесят";
    private static final String WORD_70 = "сімдесят";
    private static final String WORD_80 = "вісімдесят";
    private static final String WORD_90 = "дев'яносто";

    private static final String WORD_ENG_20 = "twenty";
    private static final String WORD_ENG_30 = "thirty";
    private static final String WORD_ENG_40 = "forty";
    private static final String WORD_ENG_50 = "fifty";
    private static final String WORD_ENG_60 = "sixty";
    private static final String WORD_ENG_70 = "seventy";
    private static final String WORD_ENG_80 = "eighty";
    private static final String WORD_ENG_90 = "ninety";

    private static final String WORD_10 = "десять";
    private static final String WORD_11 = "одинадцать";
    private static final String WORD_12 = "дванадцать";
    private static final String WORD_13 = "тринадцать";
    private static final String WORD_14 = "чотирнадцать";
    private static final String WORD_15 = "п'ятнадцать";
    private static final String WORD_16 = "шістнадцать";
    private static final String WORD_17 = "сімнадцать";
    private static final String WORD_18 = "вісімнадцать";
    private static final String WORD_19 = "дев'ятнадцать";

    private static final String WORD_ENG_10 = "ten";
    private static final String WORD_ENG_11 = "eleven";
    private static final String WORD_ENG_12 = "twelve";
    private static final String WORD_ENG_13 = "thirteen";
    private static final String WORD_ENG_14 = "fourteen";
    private static final String WORD_ENG_15 = "fifteen";
    private static final String WORD_ENG_16 = "sixteen";
    private static final String WORD_ENG_17 = "seventeen";
    private static final String WORD_ENG_18 = "eighteen";
    private static final String WORD_ENG_19 = "nineteen";


    private static final String WORD_0 = "нуль";
    private static final String WORD_1_MALE = "один";
    private static final String WORD_1_FEMALE = "одна";
    private static final String WORD_2_MALE = "два";
    private static final String WORD_2_FEMALE = "дві";
    private static final String WORD_3 = "три";
    private static final String WORD_4 = "чотири";
    private static final String WORD_5 = "п'ять";
    private static final String WORD_6 = "шість";
    private static final String WORD_7 = "сім";
    private static final String WORD_8 = "вісімь";
    private static final String WORD_9 = "дев'ять";

    private static final String WORD_ENG_0 = "zero";
    private static final String WORD_ENG_1 = "one";
    private static final String WORD_ENG_2 = "two";
    private static final String WORD_ENG_3 = "three";
    private static final String WORD_ENG_4 = "four";
    private static final String WORD_ENG_5 = "five";
    private static final String WORD_ENG_6 = "six";
    private static final String WORD_ENG_7 = "seven";
    private static final String WORD_ENG_8 = "eight";
    private static final String WORD_ENG_9 = "nine";

    private static final String THOUSAND_ONE = "тисяча";
    private static final String THOUSAND_TWO = "тисячі";
    private static final String THOUSAND_FIVE = "тисяч";

    private static final String MILLION_ONE = "мільйон";
    private static final String MILLION_TWO = "мільйони";
    private static final String MILLION_FIVE = "мільйонів";

    private static final String BILLION_ONE = "мільярд";
    private static final String BILLION_TWO = "мільярда";
    private static final String BILLION_FIVE = "мільярдів";

    private static final String THOUSAND_ENG_ONE = "thousand";
    private static final String MILLION_ENG_ONE = "million";
    private static final String BILLION_ENG_ONE = "billion";
    private static final String THOUSAND_ENG_TWO = "thousands";
    private static final String MILLION_ENG_TWO = "millions";
    private static final String BILLION_ENG_TWO = "billions";
}
