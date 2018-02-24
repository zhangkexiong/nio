package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * nio的服务器端
 */
public class NioServer {
    //接收和发送数据的缓冲区
    private ByteBuffer send = ByteBuffer.allocate(1024);
    private ByteBuffer receive = ByteBuffer.allocate(1024);

    //服务器端的通道
    ServerSocketChannel serverSocketChannel = null;
    //选择器,用来轮询使用的
    Selector selector = null;

    //构造方法
    public NioServer(int port) throws IOException {
        //打开服务器套接字通道
        serverSocketChannel = ServerSocketChannel.open();
        //服务器配置为非阻塞
        serverSocketChannel.configureBlocking(false);
        //检索与此服务器关联的套接字
        ServerSocket serverSocket = serverSocketChannel.socket();
        //进行服务的绑定
        serverSocket.bind(new InetSocketAddress(port));
        //通过open方法找到Selector
        selector = Selector.open();
        //将服务注册到selector进行监听,等待客户端的连接
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server Start ==== " + port);

        //向缓冲区中加入数据
        send.put("date come from server".getBytes());

    }

    /**
     * 监听函数
     */
    private void listener() throws IOException {
        while (true){
            //选择一组键,并且相应的管道已经打开
            selector.select();
            //返回此选择键已经选择的键集
            Set<SelectionKey> selectionKeys = selector.keys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()){
                SelectionKey selectionKey = iterator.next();
                //手动将remove掉,不然selectionKey集合不会自动去除
                iterator.remove();
                //处理相应的业务逻辑
                handle(selectionKey);
            }
        }

    }

    /**
     * 处理相应的业务逻辑
     * @param selectionKey
     */
   private void handle(SelectionKey selectionKey) throws IOException {
       ServerSocketChannel server = null;
       SocketChannel client = null;
       String receiveText;
       String sendText;
       int count = 0;
       //测试此键的通道是否已经准备好新的套接字连接
       if (selectionKey.isAcceptable()){
           //返回之前创建此键的通道
           server = (ServerSocketChannel) selectionKey.channel();
           //此方法返回的是套接字通道,如果有将处于阻塞模式
           client = server.accept();
           //配置为非阻塞状态
           client.configureBlocking(false);
           //注册到selector,等待连接
           client.register(selector,SelectionKey.OP_READ|SelectionKey.OP_WRITE);
       }else if (selectionKey.isReadable()){
           // 返回为之创建此键的通道。
           client = (SocketChannel) selectionKey.channel();
           // 将缓冲区清空以备下次读取
           receive.clear();
           // 读取服务器发送来的数据到缓冲区中
           client.read(receive);

           System.out.println(new String(receive.array()));

           selectionKey.interestOps(SelectionKey.OP_WRITE);
       }else if (selectionKey.isWritable()){
           // 将缓冲区清空以备下次写入
           send.flip();
           // 返回为之创建此键的通道。
           client = (SocketChannel) selectionKey.channel();

           // 输出到通道
           client.write(send);

           selectionKey.interestOps(SelectionKey.OP_READ);
       }

   }

   public static void main(String args[]) throws Exception{
       new NioServer(8888).listener();
   }

}
