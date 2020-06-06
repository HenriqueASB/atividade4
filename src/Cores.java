public class Cores {

    private static int cores[] ={ 0xdbead5, 0xb7d5ac, 0x93bf85, 0x6eaa5e, 0x469536, 0x008000, 0x0f6a08, 0x14540d, 0x15400e, 0x132c0d, 0x0f1a07, 0x000000 };

    public static int getCor(int i){
        if(i<12){
            return cores[i];
        }else {
            i= corrigeNumero(i);
            return cores[i];
        }
    }
    private static int corrigeNumero(int i){
        while (i>=12){
            i = i -12;
        }
        return i;
    }
}
