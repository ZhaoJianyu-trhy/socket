package server;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Set;

public class ChatServer {

    private static final int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER = 1024;

    private ServerSocketChannel server;
    private Selector selector;
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER);
    private Charset charset = Charset.forName("UTF-8");
    private int port;

    public ChatServer() {
        this(DEFAULT_PORT);
    }

    /**
     * 自定义端口
     * @param port
     */
    public ChatServer(int port) {
        this.port = port;
    }

    private void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);//设置为非阻塞
            server.socket().bind(new InetSocketAddress(port));

            selector = Selector.open();
            server.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("启动服务器，监听端口：" + port + "~~~");

            while (true) {
                selector.select();//如果没有事件发生，阻塞
                Set<SelectionKey> selectionKeys = selector.selectedKeys();//获取监听到的事件集合
                for (SelectionKey selectionKey : selectionKeys) {
                    //处理被触发的事件
                    handles(selectionKey);
                }
                selectionKeys.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClosedSelectorException e) {
            //用户正常退出
            System.err.println("已下线~~~");
        } finally {
            close(selector);
        }
    }

    private void handles(SelectionKey selectionKey) throws IOException {
        //ACCEPT事件——和客户端建立连接
        if (selectionKey.isAcceptable()) {
            ServerSocketChannel server = (ServerSocketChannel) selectionKey.channel();
            SocketChannel client = server.accept();
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ);
            System.out.println(getClientName(client) + " 已连接");
        } else if (selectionKey.isReadable()) {
            //READ事件，客户端发送消息
            SocketChannel client = (SocketChannel) selectionKey.channel();
            String fwdMsg = receive(client);
            if (fwdMsg.isEmpty()) {
                //客户端异常，取消注册
                selectionKey.cancel();
                //更新selector
                selector.wakeup();
            } else {
                //正常的消息，转发数据
                forwardMsg(client, fwdMsg);
                //检查用户是否退出
                if (readyToQuit(fwdMsg)) {
                    //取消注册
                    selectionKey.cancel();
                    selector.wakeup();
                    System.out.println(getClientName(client) + " 已断开");
                }
            }
        }
    }

    private void forwardMsg(SocketChannel client, String fwdMsg) throws IOException {
        for (SelectionKey key : selector.keys()) {
            SelectableChannel connectedChannel = key.channel();
            if (connectedChannel instanceof ServerSocketChannel) {
                continue;
            }
            if (key.isValid() && !client.equals(connectedChannel)) {
                wBuffer.clear();
                wBuffer.put(charset.encode(getClientName(client) + "：" + fwdMsg));
                wBuffer.flip();
                while (wBuffer.hasRemaining()) {
                    ((SocketChannel) connectedChannel).write(wBuffer);
                }
            }
        }
    }

    private String receive(SocketChannel client) throws IOException {
        rBuffer.clear();
        while (client.read(rBuffer) > 0);
        rBuffer.flip();
        return String.valueOf(charset.decode(rBuffer));
    }

    private boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    private String getClientName(SocketChannel client) {
        return "客户端[" + client.socket().getPort() + "]";
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer(7777);
        chatServer.start();
    }
}
