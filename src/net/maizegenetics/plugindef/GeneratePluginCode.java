/*
 *  GeneratePluginCode
 */
package net.maizegenetics.plugindef;

import java.awt.Frame;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import net.maizegenetics.util.Utils;
import org.apache.log4j.Logger;

/**
 *
 * @author Terry Casstevens
 */
public class GeneratePluginCode {

    private static final Logger myLogger = Logger.getLogger(GeneratePluginCode.class);

    private GeneratePluginCode() {
    }

    public static void generate(Class currentMatch) {
        try {
            Constructor constructor = currentMatch.getConstructor(Frame.class);
            generate((AbstractPlugin) constructor.newInstance((Frame) null));
        } catch (Exception ex) {
            try {
                Constructor constructor = currentMatch.getConstructor(Frame.class, boolean.class);
                generate((AbstractPlugin) constructor.newInstance(null, false));
            } catch (NoSuchMethodException nsme) {
                myLogger.warn("Self-describing Plugins should implement this constructor: " + currentMatch.getClass().getName());
                myLogger.warn("public Plugin(Frame parentFrame, boolean isInteractive) {");
                myLogger.warn("   super(parentFrame, isInteractive);");
                myLogger.warn("}");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void generate(AbstractPlugin plugin) {
        String clazz = Utils.getBasename(plugin.getClass().getName());

        System.out.println("    // The following getters and setters were auto-generated.");
        System.out.println("    // Please use this method to re-generate.");
        System.out.println("    //");
        System.out.println("    // public static void main(String[] args) {");
        System.out.println("    //     GeneratePluginCode.generate(" + clazz + ".class);");
        System.out.println("    // }");
        System.out.println("");

        System.out.println("    /**");
        System.out.println("     * Convenience method to run plugin with one return object.");
        System.out.println("     */");
        System.out.println("    // TODO: Replace <Type> with specific type.");
        System.out.println("    public <Type> runPlugin(DataSet input) {");
        System.out.println("        return (<Type>) performFunction(input).getData(0).getData();");
        System.out.println("    }\n");

        for (Field field : plugin.getParameterFields()) {
            PluginParameter<?> current = null;
            try {
                current = (PluginParameter) field.get(plugin);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
            String guiNameAsCamelCase = stringToCamelCase(current.guiName());

            //Getter
            System.out.println("    /**");
            System.out.println(createDescription(current.description()));
            System.out.println("     *");
            System.out.println("     * @return " + current.guiName());
            System.out.println("     */");
            System.out.println("    public " + current.valueType().getSimpleName() + " " + guiNameAsCamelCase + "() {");
            System.out.println("        return " + field.getName() + ".value();");
            System.out.println("    }\n");

            // Setter
            System.out.println("    /**");
            System.out.println(createDescription("Set " + current.guiName() + ". " + current.description()));
            System.out.println("     *");
            System.out.println("     * @param value " + current.guiName());
            System.out.println("     *");
            System.out.println("     * @return this plugin");
            System.out.println("     */");
            System.out.println("    public " + clazz + " " + guiNameAsCamelCase + "(" + current.valueType().getSimpleName() + " value) {");
            System.out.println("        " + field.getName() + " = new PluginParameter<>(" + field.getName() + ", value);");
            System.out.println("        return this;");
            System.out.println("    }\n");
        }
    }

    private static final int DEFAULT_DESCRIPTION_LINE_LENGTH = 50;

    private static String createDescription(String description) {
        int count = 0;
        StringBuilder builder = new StringBuilder();
        builder.append("     * ");
        for (int i = 0, n = description.length(); i < n; i++) {
            count++;
            if (description.charAt(i) == '\n') {
                builder.append("\n");
                builder.append("     * ");
                count = 0;
            } else if ((count > DEFAULT_DESCRIPTION_LINE_LENGTH) && (description.charAt(i) == ' ')) {
                builder.append("\n");
                builder.append("     * ");
                count = 0;
            } else {
                builder.append(description.charAt(i));
            }
        }
        return builder.toString();
    }

    private static String stringToCamelCase(String str) {
        StringBuilder builder = new StringBuilder();
        builder.append(Character.toLowerCase(str.charAt(0)));
        boolean makeUpper = false;
        for (int i = 1; i < str.length(); i++) {
            char current = str.charAt(i);
            if (current == ' ') {
                makeUpper = true;
            } else if (makeUpper) {
                builder.append(Character.toUpperCase(current));
                makeUpper = false;
            } else {
                builder.append(current);
            }
        }
        return builder.toString();
    }

}
