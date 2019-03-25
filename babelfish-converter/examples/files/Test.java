import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Test {
    public static void main(String[] args) {
        System.out.println(matches("0000000000000000", "},{", 10));
    }

    private static boolean matches(String text, String regex, long timeoutInMs) {
        if (text == null || regex == null) {
            return false;
        }

        Pattern pattern;
        try {
            pattern = Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            return false;
        }

        final Matcher matcher = pattern.matcher(text);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<Boolean> future = executorService.submit(matcher::matches);
        executorService.shutdown();

        boolean result = false;
        try {
            result = future.get(timeoutInMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
        } catch (InterruptedException | ExecutionException ignored) {
        }

        return result;
    }

    private static void test() {
        System.out.println();

    }

    public class MyString implements CharSequence {

        private String text;
        private long duration;
        private long startTime;

        public MyString(String text, long duration) {

            this.text = text;
            this.duration = duration;
            this.startTime = System.currentTimeMillis();
        }

        @Override
        public int length() {
            return text.length();
        }

        @Override
        public char charAt(int i) {
            if (System.currentTimeMillis() - startTime > duration) {
                throw new RuntimeException();
            }
            return text.charAt(i);
        }

        @Override
        public CharSequence subSequence(int i, int i1) {
            return text.subSequence(i, i1);
        }
    }
}
