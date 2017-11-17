import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

  public static final int PORT = 8585;

  public static void main(String[] args) throws Exception {
    int count = 4;
    Selector selector = Selector.open();

    ExecutorService executorService = Executors.newSingleThreadExecutor();


    for (int i = 0; i < count; i++) {
      executorService.submit(new Client());
      System.out.println("for cycle");
      ServerSocketChannel ssc = selector.provider().openServerSocketChannel();
      ssc.bind(new InetSocketAddress(PORT));
      ssc.configureBlocking(false);
      ssc.register(selector, SelectionKey.OP_ACCEPT);
      int acceptCount = 0;
      while (true) {
        int keysCount = selector.select();
        if (keysCount <= 0) {
          continue;
        }
        Set<SelectionKey> selectionKeys = selector.selectedKeys();

        for (SelectionKey key : selectionKeys) {
          if (key.isAcceptable()) {
            acceptCount++;
            ((ServerSocketChannel)key.channel()).accept().close();
          }
        }
        System.out.println("accept count " + acceptCount);
        selectionKeys.clear();
        if (acceptCount == 2) {
          break;
        }
      }
      // TODO: 11/17/17
      //не освобождает порт на винде. Скорее всего надо так же выполнить selector.close()
      //Стоит проверить так же, можно ли не закрывая селектор решить проблему (например ssc.socket().close() или какой-нибудь unbind/unregister)
      //И провести тест на утечку каналов: Если открывать кучу клиентских каналов, читать с них данные и закрывать
      //Не будут ли они утекать. Можно добавить в attachment большой объект, чтобы больше памяти жрало
      ssc.close();
    }
    executorService.shutdownNow();
  }

  public static class Client implements Runnable {


    @Override
    public void run() {
      try {
        SocketChannel sc = SocketChannel.open();
        sc.configureBlocking(false);
        sc.connect(new InetSocketAddress(PORT));
        Thread.sleep(3000);
        sc = SocketChannel.open();
        sc.configureBlocking(false);
        sc.connect(new InetSocketAddress(PORT));
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
  }
}
