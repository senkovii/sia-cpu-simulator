import java.util.HashMap;
import java.util.LinkedList;

public class Assembler {
    private static HashMap<String, String> opcodes;
    private static HashMap<String, String> registers;

    static {
        opcodes = new HashMap<>();
        registers = new HashMap<>();

        for (int i = 0; i <= 31; i++) {
            registers.put("r" + i, toBinary(i,5));
        }

        opcodes.put("halt", toBinary(0,5));
        opcodes.put("add", toBinary(1,5));
        opcodes.put("and", toBinary(2,5));
        opcodes.put("multiply", toBinary(3,5));
        opcodes.put("leftshift", toBinary(4,5));
        opcodes.put("subtract", toBinary(5,5));
        opcodes.put("or", toBinary(6,5));
        opcodes.put("rightshift", toBinary(7,5));
        opcodes.put("syscall", toBinary(8,5));
        opcodes.put("call", toBinary(9,5));
        opcodes.put("return", toBinary(10,5));
        opcodes.put("compare", toBinary(11,5));
        opcodes.put("ble", toBinary(12,5));
        opcodes.put("blt", toBinary(13,5));
        opcodes.put("bge", toBinary(14,5));
        opcodes.put("bgt", toBinary(15,5));
        opcodes.put("beq", toBinary(16,5));
        opcodes.put("bne", toBinary(17,5));
        opcodes.put("load", toBinary(18,5));
        opcodes.put("store", toBinary(19,5));
        opcodes.put("copy", toBinary(20,5));
    }

    public static String[] assemble(String[] input) {
        String[] output = new String[input.length];

        for (int i = 0; i < input.length; i++) {
            String[] tokens = input[i].split(" "); // split current line

            if (tokens.length == 1) {        // halt/return
                output[i] = opcodes.get(tokens[0]) + "00000000000";
            } else if (tokens.length == 2) { // call with number
                output[i] = opcodes.get(tokens[0]) + toBinary(Integer.parseInt(tokens[1]), 11);
            } else if (tokens.length == 3) {
                if (registers.containsKey(tokens[1])) {  // 2R format
                    output[i] = opcodes.get(tokens[0]) + "0" + registers.get(tokens[1]) + registers.get(tokens[2]);
                } else {                                  // Immediate format
                    output[i] = opcodes.get(tokens[0]) + "1" + toBinary(Integer.parseInt(tokens[1]), 5) + registers.get(tokens[2]);
                }
            }
        }
        return output;
    }

    public static String[] finalOutput(String[] input) {
        String[] output = new String[(input.length + 1) / 2];   //round up

        for (int i = 0; i < input.length; i += 2) {
            if (i + 2 > input.length) {  // odd case - no pair exists
                output[i/2] = input[i] + "0000000000000000";
            } else {    // normal case - pair exists
                output[i/2] = input[i] + input[i + 1];
            }
        }
        return output;
    }

    public static String toBinary(int value, int bits) {
        String result = "";
        for (int i = 0; i < bits; i++) {
            int rmBit = value & 1;  // grab the rightmost bit of value
            result = rmBit + result;   // prepend to result (build right->left)
            value = value >> 1;     // shift right by 1
        }
        return result;
    }
}
