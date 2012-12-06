package ha_util;

public class HA_utils 
{
    public String [] get_cmd_List (String inString, String delims)
    {
         String[] tokens = inString.split(delims);
         return tokens;
    }

    public int get_strings_count (String inString)
    {
        int count = 0 ;
        for(int i = 0; i < inString.length(); i++) 
        {
            if(Character.isWhitespace(inString.charAt(i))) count++;
        }
        return count;
    }

    public int [] String2int (String inString, int [] int_arr, int count)
    {
        String[] tokens = inString.split("[ ]");
        for(int i = 0; i <= count; i++) 
        {
            int_arr [i] = Integer.parseInt(tokens[i]);
        }
        return int_arr;
    }

    public int get_List_entries_count (byte [] payload, byte j)
    {
        int entries_counter = 0; 	
        for (int i = 0 ; i < payload.length; i++)
        {
           if ( j == payload [i]) entries_counter++;
        }
        return entries_counter;
    }

    public String cutString (String inString)
    {   
        if ( inString.length () > 0)
        {
            StringBuilder strB = new StringBuilder(inString);
            strB.deleteCharAt(inString.length() - 1);
            return strB.toString();
        }
        else
            return "";
    }
    
    public boolean isNumericSpace(String str) 
    {
        if (str == null) 
        {
            return false;
        }
        
        int sz = str.length();
        
        for (int i = 0; i < sz; i++) 
        {
            if ((Character.isDigit(str.charAt(i)) == false) && 
                (str.charAt(i) != ' ') && str.charAt(i) != '\t') 
            {
                return false;
            }
        }
        return true;
    }
}
