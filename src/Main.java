import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        boolean theEndProgram = false;

        while (!theEndProgram) {
            System.out.println("Please, choose the language: UA or ENG. If you want exit enter 'y'. ");
            String language = readConsoleElement();
            if (language.equalsIgnoreCase("y")){
                theEndProgram = true;
            }
            else if (language.equals("UA") || language.equals("ENG")) {
                while (true){
                System.out.println("Please, enter the value from 0 to 2147483647. Use format for example 9,00. Remember that symbol '.' use for ENG and ',' for UA.");
                String value = readConsoleElement();
                if (language.equals("UA") &&  value.matches("^([,\\d]+)([,]\\d{2})$")) {
                    value = value.replaceAll("[,]", "");
                    Long sum = Long.parseLong(value);
                    System.out.println(AmountInWords.format(sum, AmountInWords.UAH, "UA"));
                    break;
                } else if (language.equals("ENG") && value.matches("^([.\\d]+)([.]\\d{2})$")) {
                    value = value.replaceAll("[.]", "");
                    Long sum = Long.parseLong(value);
                    System.out.println(AmountInWords.format(sum, AmountInWords.USD, "ENG"));
                    break;
                }
                else { System.out.println("You wrote wrong value. Please try again"); }
                }

            }else System.out.println("Incorrect command. Please try again");


        }
    }

    public static String readConsoleElement() {
        String value;
        Scanner scanner = new Scanner(System.in);
        value = scanner.nextLine();
        return value;
    }



}
