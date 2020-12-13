package ZTE;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

/**
 * 最弱鸡的方式（捂脸）：
 * 尽可能的不用拣货员，则需要将同源同宿的货物集合为重量等于列车最大承载的一个大货物。
 * 然后针对每一个大货物根据源节点和目的节点利用迪杰斯特拉算法计算最短路径，
 * 然后从整条路径中挑选一个特定列车运输，列车在途中不变更。故只需要在源，
 * 目的物流中转站配有装货和卸货两个分拣员。最终找不到路径的，或者找到路径无
 * 法找到一致的列车编号的，则抛弃尽可能的不用拣货员，则需要将同源同宿的货物集
 * 合为重量等于列车最大承载的一个大货物。然后针对每一个大货物根据源节点和目的
 * 节点利用迪杰斯特拉算法计算最短路径，然后从整条路径中挑选一个特定列车运输，
 * 列车在途中不变更。故只需要在源，目的物流中转站配有装货和卸货两个分拣员。
 * 最终找不到路径的，或者找到路径无法找到一致的列车编号的，则抛弃。
 */
public class ZTEchallenge02 implements Cloneable {
    public Map<String, Node> nodeMap = new HashMap<>();
    public Map<String, Edge> linkMap = new HashMap<>();
    public Map<Node, List<Edge>> ver_edgeList_map = new HashMap<>(); //邻接表
    public HashMap<goodPrefix, ArrayList<String>> mergeGoods = new HashMap<>();//同源同宿大货物，源宿是key，小货物集合是value
    public HashMap<goodPrefix, Double> mergeWeight = new HashMap<>();//同源同宿大货物，源宿是key，货物重量是value
    public Map<String,Double> requestMap=new HashMap<>();
    public Map<Src2dst,Edge> link2Edage=new HashMap<>();

    public ZTEchallenge02(Map<String, Node> nodeMap, Map<String, Edge> linkMap, Map<Node, List<Edge>> ver_edgeList_map,Map<String,Double> requestMap,Map<Src2dst,Edge> link2Edage) {
        this.nodeMap = nodeMap;
        this.linkMap = linkMap;
        this.ver_edgeList_map = ver_edgeList_map;
        this.requestMap=requestMap;
        this.link2Edage=link2Edage;
    }

    static class Src2dst{
        public String source;
        public String dest;

        public Src2dst(String source, String dest) {
            this.source = source;
            this.dest = dest;
        }
        @Override
        public int hashCode() {
            return (source + dest).hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this.source.equals(((Src2dst) obj).source) && this.dest.equals(((Src2dst) obj).dest)) {
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return "Src2dst{" +
                    "source='" + source + '\'' +
                    ", dest='" + dest + '\'' +
                    '}';
        }
    }
    static class goodPrefix{
        public String source; //源
        public String dest; //宿
        int index; //编号：因为同源同宿小货物不光可以汇聚成一个大货物，可能有多个

        public goodPrefix(String source, String dest, int index) {
            this.source = source;
            this.dest = dest;
            this.index = index;
        }

        @Override
        public int hashCode() {
            return (source + dest+String.valueOf(index)).hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this.source.equals(((goodPrefix) obj).source) && this.dest.equals(((goodPrefix) obj).dest)&&this.index==((goodPrefix) obj).index) {
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return "goodPrefix{" +
                    "source='" + source + '\'' +
                    ", dest='" + dest + '\'' +
                    ", index=" + index +
                    '}';
        }
    }


    static class Edge {
        public String linkName;
        public Node startNode;
        public Node endNode;
        public int weight;
        public double[] channels;
        public int channelNum;
        public double capacity;

        public Edge() {

            this.weight = 1;
        }

        public Edge(String linkName, Node startNode, Node endNode, int channelNum, double capacity) {
            this();
            this.linkName = linkName;
            this.startNode = startNode;
            this.endNode = endNode;
            this.channelNum = channelNum+1;
            this.channels = new double[channelNum];
            this.capacity = capacity;
            Arrays.fill(channels, capacity);

        }


        @Override
        public String toString() {
            return "Edge{" +
                    "linkName='" + linkName + '\'' +
                    ", startNode=" + startNode +
                    ", endNode=" + endNode +
                    ", weight=" + weight +
                    ", channels=" + Arrays.toString(channels) +
                    ", channelNum=" + channelNum +
                    ", capacity=" + capacity +
                    '}';
        }
    }

    /**
     * Dijkstra用到了优先级队列，重写了compareTo方法
     */
    static class Node implements Comparable<Node> {
        public String name;
        public Node parent;
        public Integer worker;
        public int dist;


        public Node() {

            this.parent = null;

        }

        public Node(String name, Integer dist, Integer worker) {
            this();
            this.name = name;
            this.worker = worker;
            this.dist = dist;
        }

        @Override
        public int hashCode() {
            return this.name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Node)) {
                throw new ClassCastException("an object to compare with a Node must be Node");
            }

            if (this.name == null) {
                throw new NullPointerException("name of Node to be compared cannot be null");
            }

            return this.name.equals(((Node) obj).name);
        }

        @Override
        public String toString() {
            return "Node{" +
                    "name='" + name + '\'' +
                    ", parent=" + parent +
                    ", worker=" + worker +
                    ", dist=" + dist +
                    '}';
        }

        @Override
        public int compareTo(Node o) {
            if (this.dist < o.dist) {
                return -1;
            } else if (this.dist > o.dist) {
                return 1;
            } else {
                return this.name.compareTo(o.name);
            }
        }
    }

    /**
     * Dijkstra算法
     * @param start
     * @param end
     * @return
     */
    public List<String> getShortestPath(String start, String end) {
        final Map<String, Integer> distance = new HashMap<>();
        final Map<String, Node> previous = new HashMap<>();
        PriorityQueue<Node> nodes = new PriorityQueue<>();
        for (String nodeStr : nodeMap.keySet()) {
            String nname = nodeStr;
            int worker = nodeMap.get(nodeStr).worker;
            if (nname.equals(start)) {
                distance.put(nname, 0);
                nodes.add(new Node(nname, 0, worker));
            } else {
                distance.put(nname, Integer.MAX_VALUE);
                nodes.add(new Node(nname, Integer.MAX_VALUE, worker));
            }
            previous.put(nname, null);

        }
        while (!nodes.isEmpty()) {
            Node smallest = nodes.poll();
            if (smallest.name.equals(end)) {
                final List<String> path = new ArrayList<>();

                while ((previous.get(smallest.name)) != null) {
                    path.add(smallest.name);
                    smallest = previous.get(smallest.name);
                }
                path.add(start);
                Collections.reverse(path);
                return path;
            }
            if (distance.get(smallest.name) == Integer.MAX_VALUE) {
                break;
            }
            if (!ver_edgeList_map.containsKey(smallest)) {
                continue;
            }

            for (Edge edge : ver_edgeList_map.get(smallest)) {
                Node edgeEndNode = edge.endNode;
                int alt = distance.get(smallest.name) + edgeEndNode.dist;
                if (alt < distance.get(edgeEndNode.name)) {
                    distance.put(edgeEndNode.name, alt);
                    previous.put(edgeEndNode.name, smallest);
                    forloop:
                    for (Node n : nodes) {
                        if (n.name.equals(edgeEndNode.name)) {
                            nodes.remove(n);
                            n.dist = alt;
                            nodes.add(n);
                            break forloop;
                        }
                    }
                }

            }
        }
        return null;
    }

    public static void main(String[] args) {
        Map<String, Node> nodeMap = new HashMap<>();
        Map<String, Edge> linkMap = new HashMap<>();
        Map<Node, List<Edge>> ver_edgeList_map = new HashMap<>();;
        HashMap<goodPrefix, ArrayList<String>> mergeGoods = new HashMap<>();
        HashMap<goodPrefix, Double> mergeWeight = new HashMap<>();
        Map<Src2dst,Edge> link2Edage=new HashMap<>();
        HashMap<String, ArrayList<String>> result = new HashMap<>();
        Map<String,Double> requestMap=new HashMap<>();
        double allWeights=0;
        double channelCapacity=0;
        int channel=0;
        int fail=0;
        /**
         * 以下，文件读取并插入相关数据到上述map
         */
        try {
//            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            BufferedReader in = new BufferedReader(new FileReader("D:\\LeeCode\\src\\ZTE\\test.txt"));
            String s = "";
            s = in.readLine();
            String[] dataSize = s.split(",");
            int nodeNum = Integer.parseInt(dataSize[0]);
            int linkNum = Integer.parseInt(dataSize[1]);
            int channelNum = Integer.parseInt(dataSize[2]);
            channel=channelNum;
            double maxCapacity = Double.parseDouble(dataSize[3]);
            channelCapacity=maxCapacity;
            for (int i = 0; i < nodeNum; i++) {
                s = in.readLine();
                String[] temp = s.split(",");
                nodeMap.put(temp[0], new Node(temp[0], 1, Integer.parseInt(temp[1])));
            }
            for (int i = 0; i < linkNum; i++) {
                s = in.readLine();
                String[] temp = s.split(",");
                Node tempNode = nodeMap.get(temp[1]);
                Node tempNode2 = nodeMap.get(temp[2]);
                Src2dst sd=new Src2dst(temp[1],temp[2]);
                String linkName = temp[1] + "-" + temp[2];
                Edge edge = new Edge(temp[0], tempNode, tempNode2, channelNum, maxCapacity);
                link2Edage.put(sd, edge);
                linkMap.put(temp[0], edge);
                if (ver_edgeList_map.containsKey(tempNode)) {
                    ver_edgeList_map.get(tempNode).add(edge);
                } else {
                    List<Edge> list = new ArrayList<>();
                    list.add(edge);
                    ver_edgeList_map.put(tempNode, list);
                }
            }
            s = in.readLine();
            boolean flag=true;
            int index=0;
            int requestNum = Integer.parseInt(s);
            for (int i = 0; i < requestNum; i++) {
                flag = false;
                int j = index;
                s = in.readLine();
                String[] temp = s.split(",");

                goodPrefix p = new goodPrefix(temp[1], temp[2],j);
                double w = Double.parseDouble(temp[3]);
                if ("null".equals(temp[4])) {
                    requestMap.put(temp[0],Double.parseDouble(temp[3]));
                    while (mergeGoods.containsKey(p) && mergeWeight.containsKey(p)) {
                        double weight = mergeWeight.get(p);
                        if (weight + w <= maxCapacity) {
                            mergeWeight.put(p, weight + w);
                            mergeGoods.get(p).add(temp[0]);
                            flag = true;
                            break;
                        }
                        j++;
                        p = new goodPrefix(temp[1], temp[2],j);
                    }
                    if (!flag) {
                        ArrayList<String> list = new ArrayList<>();
                        list.add(temp[0]);
                        mergeGoods.put(p, list);
                        mergeWeight.put(p, w);
                    }
                }else{
                    ArrayList r=new ArrayList();
                    r.add("null");
                    r.add("null");
                    result.put(temp[0],r);
                    fail++;
                    allWeights+=Double.parseDouble(temp[3]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        /**
         * ------------------终于读取完毕------------------
         */


        ZTEchallenge02 G = new ZTEchallenge02(nodeMap, linkMap, ver_edgeList_map,requestMap,link2Edage);
        for (Map.Entry<goodPrefix, ArrayList<String>> goods : mergeGoods.entrySet()) { //遍历大货物集合规划
            String source = goods.getKey().source;
            String dest = goods.getKey().dest;
            double w=mergeWeight.get(goods.getKey());
            String p;
            String c="null";
            ArrayList<String> goodslist = goods.getValue();
            ArrayList<String> channelIndex=new ArrayList<>();
            List<String> sb = new ArrayList<>();
            List<String> path = G.getShortestPath(source, dest);
            if(path==null){  //路径为空，直接抛弃
                p="null";
                fail+=mergeGoods.get(goods.getKey()).size();
                allWeights+=w;
                for (String g : goodslist) { //遍历大货物的小货物，依次处理
                    ArrayList<String> r=new ArrayList<>();
                    r.add(p);
                    r.add(c);
                    result.put(g, r);
                }
                continue;
            }
            Node srcNode=nodeMap.get(path.get(0));
            Node dstNode=nodeMap.get(path.get(path.size()-1));
            if(srcNode.worker<=0||dstNode.worker<=0){  //源宿的分拣员没了，就抛弃
                p="null";
                fail+=mergeGoods.get(goods.getKey()).size();
                allWeights+=w;
            for (String g : goodslist) {
            ArrayList<String> r=new ArrayList<>();
                r.add(p);
                r.add(c);
                result.put(g, r);
            }
                continue;
            }
            /**
             * 遍历所有列车数目，获得列车序号，然后遍历路径，如果路径上所有轨道都能找到同一序号的列车可以没被使用（如果是使用过，但是剩余空间够，就得考虑汇聚问题，本菜鸡没写出来）
             * 则利用这个列车规划，否则继续遍历，如果找不到就抛弃
             */
            for (int i = 1; i < channel+1; i++) {
                for (int j = 0; j <path.size()-1 ; j++) {
                    Src2dst sd = new Src2dst(path.get(j), path.get(j + 1));
                    Edge e = link2Edage.get(sd);
                    if(e.channels[i]!=channelCapacity){
                        break;
                    }
                    sb.add(e.linkName);
                }
                if(sb.size()!=path.size()-1){
                    sb.clear();
                }else{
                    for (int j = 0; j <path.size()-1 ; j++) {
                        channelIndex.add(String.valueOf(i));
                    }
                    break;
                }
            }
            if(sb.size()==path.size()-1){
                for (int i = 0; i <sb.size();i++) { //将路径上对应列车载重减去货物重量
                    Edge e = linkMap.get(sb.get(i));
                    String k=channelIndex.get(i);
                    e.channels[Integer.parseInt(k)]-=w;
                }
                    p=String.join(",",sb);
                    c = String.join(",", channelIndex);
                    srcNode.worker -= 1;
                    dstNode.worker -= 1;
            }else{
                p="null";
                fail+=mergeGoods.get(goods.getKey()).size();
                allWeights+=w;
            }
             for (String g : goodslist) {
                    ArrayList<String> r = new ArrayList<>();
                    r.add(p);
                    r.add(c);
                    result.put(g, r);
                }
        }
        DecimalFormat df=new DecimalFormat("#.000");
        System.out.println(fail+","+df.format(allWeights));
        for (Map.Entry<String, ArrayList<String>> hehe : result.entrySet()) {
            System.out.println(hehe.getKey());
            ArrayList<String> value = hehe.getValue();
            for (String s : value) {
                System.out.println(s);
            }
        }
        G.outputFile(fail, allWeights, result);
    }

    public void outputFile(int fail,double weight,Map<String,ArrayList<String>> result)  {
        BufferedWriter out=null;
        try {
            out=new BufferedWriter(new FileWriter("D:\\LeeCode\\src\\ZTE\\result.txt"));
            out.write(String.valueOf(fail)+","+String.valueOf(weight)+"\r\n");
            for (Map.Entry<String, ArrayList<String>> r : result.entrySet()) {
                out.write(r.getKey()+"\r\n");
                ArrayList<String> value = r.getValue();
                for (String s : value) {
                    out.write(s+"\r\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
        finally {
            try {
                if(out!=null){
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}





