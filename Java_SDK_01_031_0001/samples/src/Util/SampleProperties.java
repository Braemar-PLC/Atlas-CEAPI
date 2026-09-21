package Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Provides methods for setting the location of the properties file, and for
 * retrieving properties contained in the file.
 * 
 * @author <a href="mailto:developer@theice.com">ICE Data Services - Developer Support</a>
 * 
 */
public class SampleProperties {
   private static volatile SampleProperties sampleProperties;
   private Properties props;
   private String fileLocation = "";
   private String helpFile = "";
   
   private String[] args;

   private SampleProperties() {
   }

   public static SampleProperties getInstance() {
      if (sampleProperties == null) {
         synchronized (SampleProperties.class) {
            if (sampleProperties == null) {
               sampleProperties = new SampleProperties();
            }
         }
      }
      return sampleProperties;
   }

	public void setFileLocation(String fileLocation) {
		this.fileLocation = fileLocation;
	}
    
   public Properties getProperties() {
	   
	   
      if (this.props == null) {
         try (InputStream is = SampleProperties.class.getResourceAsStream(this.fileLocation)) {
            props = new Properties();
            props.load(is);
         } catch (Exception e) {
            throw new IllegalStateException("Could not load properties from: " + fileLocation, e);
         }
      }
      return this.props;
   }

   public void setHelpLocation(String helpFile) {
      this.helpFile = helpFile;
   }
   
   public void displayHelp(boolean missingArguments) {
      int numLinesRead = 0;
      int btmLimit = 65; // Configurable limit
            
      if (missingArguments) {
         System.out.println("ERROR:  Missing Arguments\n\n");
      }
               
      String helpLine;

      try (InputStream is = SampleProperties.class.getResourceAsStream(this.helpFile);
           BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
         helpLine = br.readLine();

         while (helpLine != null) {
            if (numLinesRead < btmLimit) {
               numLinesRead++;
               System.out.println(helpLine);
               helpLine = br.readLine();
            } else {         
               System.out.println();
               System.out.print("-- Press ENTER --");
               System.in.read();
               System.out.print("\r");
               numLinesRead = 0;
            }
         }
      } catch (IOException e) {
         throw new IllegalStateException("Could not read help file: " + this.helpFile, e);
      }
   }

   // Method to set args for access
   public void setArgs(String[] args) {
      this.args = args.clone();
   }
   
   // Parse arguments into a map for easier access
   public Map<String, String> parseArguments() {
      Map<String, String> arguments = new HashMap<>();
      for (int i = 0; i < args.length; i++) {
         if (args[i].startsWith("-") && i + 1 < args.length && !args[i + 1].startsWith("-")) {
            arguments.put(args[i], args[i + 1]);
         }
      }
      return arguments;
   }
   
   /**
    * Helper method to format and display a setting.
    * 
    * <p>If the value is null or empty, it displays a placeholder message. If the value exceeds 
    * the "max" characters, it is broken into multiple lines for readability.</p>
    * 
    * @param label The label of the setting.
    * @param value The value of the setting.
    * @param max   The maximum length of a single line for the value; 0 indicates no length limit.
    */
   public void displaySetting(String label, String value, int max) {
       String indent = "                      "; // 22 spaces for alignment after the label

       if (value == null || value.isEmpty()) {
           // Handle null or empty values with a placeholder
           System.out.printf("%-20s: %s%n", label, "not set");
           return;
       }

       if (max > 0 && value.length() > max) {
           // Split and format value into multiple lines if it exceeds max length
           String[] words = value.split(" ");
           System.out.printf("%-20s: ", label);
           StringBuilder currentLine = new StringBuilder();
           for (String word : words) {
               if (currentLine.length() + word.length() > max) {
                   System.out.println(currentLine.toString());
                   currentLine.setLength(0); // Clear the current line
                   currentLine.append(indent); // Add indentation for subsequent lines
               }
               currentLine.append(word).append(" ");
           }
           if (currentLine.length() > 0) {
               System.out.println(currentLine.toString());
           }
       } else {
           // Print the label and value as a single line
           System.out.printf("%-20s: %-44s%n", label, value);
       }
   }

   /**
    * Overloaded helper method to format and display a setting.
    * Accepts a String[] value, joins the elements, and delegates to
    * the original method for the same wrapping/formatting behavior.
    *
    * @param label The label of the setting
    * @param values The array of strings representing the setting's value
    * @param max The maximum length of a sample setting; 0 indicates no length limit
    */
   public void displaySetting(String label, String[] values, int max) {
       // If the array is null or empty, just display an empty string
       if (values == null || values.length == 0) {
           displaySetting(label, "", max);
       } else {
           // Join all array elements into a single string, separated by commas
           String joinedValue = String.join(",", values);
           // Delegate to the original String-based method
           displaySetting(label, joinedValue, max);
       }
   }      
}
