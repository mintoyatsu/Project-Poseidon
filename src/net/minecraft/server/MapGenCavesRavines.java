package net.minecraft.server;

import java.util.Random;

public class MapGenCavesRavines
{
    private final Random rand = new Random();
    private final Random noiseGen = new Random();
    private final Random caveRNG = new Random();
    private final Random largeCaveRNG = new Random();
    private final World world;
    private final long seedMultiplier;
    private final int caveOffsetX;
    private final int caveOffsetZ;
    private int chunk_X;
    private int chunk_Z;
    private int chunkX_16;
    private int chunkZ_16;
    private double chunkCenterX;
    private double chunkCenterZ;
    private byte[] chunkData;
    private final byte[] caveDataArray = new byte[1369];
    private final float[] ravineData = new float[128];
    private static final float[] SINE_TABLE = new float[1024];

    public MapGenCavesRavines(World par1World)
    {
        this.world = par1World;
        this.rand.setSeed(this.world.getSeed());
        this.seedMultiplier = this.rand.nextLong() / 2L * 2L + 1L;
        this.caveOffsetX = this.rand.nextInt(128) + 2000000;
        this.caveOffsetZ = this.rand.nextInt(128) + 2000000;
    }

    public void generate(int chunkX, int chunkZ, byte[] data)
    {
        this.chunkData = data;
        this.chunk_X = chunkX;
        this.chunk_Z = chunkZ;
        this.chunkX_16 = chunkX * 16;
        this.chunkZ_16 = chunkZ * 16;
        this.chunkCenterX = (double)(this.chunkX_16 + 8);
        this.chunkCenterZ = (double)(this.chunkZ_16 + 8);
        this.noiseGen.setSeed((long)chunkX * 341873128712L + (long)chunkZ * 132897987541L);
        this.initializeCaveData();

        for (int x = -12; x <= 12; ++x)
        {
            for (int z = -12; z <= 12; ++z)
            {
                int x2z2 = x * x + z * z;

                if (x2z2 <= 145)
                {
                    int cx = chunkX + x;
                    int cz = chunkZ + z;

                    if (cx != 0 || cz != 0)
                    {
                        long chunkSeed = ((long)cx * 341873128712L + (long)cz * 132897987541L) * this.seedMultiplier;
                        int genCaves = this.validCaveLocation(x, z);

                        if (genCaves != 2 && genCaves < 6)
                        {
                            if (genCaves > 0)
                            {
                                if (genCaves == 3)
                                {
                                    this.rand.setSeed(chunkSeed);
                                    this.generateRegionalCaves(cx, cz);
                                }

                                this.rand.setSeed(chunkSeed);
                                this.generateCaves(cx, cz, x2z2, genCaves);
                                this.rand.setSeed(chunkSeed);
                                this.generateRavines(cx, cz, x2z2 <= 20, genCaves < 5);
                            }
                            else if (genCaves < 0 && x2z2 <= 101)
                            {
                                this.rand.setSeed(chunkSeed);
                                this.generateColossalCaveSystem(cx, cz);
                            }
                        }
                        else if (x2z2 <= 65)
                        {
                            this.rand.setSeed(chunkSeed);
                            this.generateSpecialCaveSystems(cx, cz, genCaves);
                        }
                    }
                }
            }
        }
    }

    private void generateCaves(int chunkX, int chunkZ, int flag, int genCaves)
    {
        int chance = this.rand.nextInt(15);
        int caveSize = 0;
        int blockX;
        int blockZ;
        int type;
        int range;
        int i;
        BiomeBase biomebase;

        if (chance == 0)
        {
            caveSize = this.rand.nextInt(this.rand.nextInt(this.rand.nextInt(40) + 1) + 1);

            if (caveSize > 0)
            {
                blockX = chunkX * 16;
                blockZ = chunkZ * 16;
                type = chunkX + this.caveOffsetX + 4;
                range = chunkZ + this.caveOffsetZ + 4;
                i = -1;
                boolean genNormalCaves = genCaves != 3 && genCaves != 4;
                boolean applyCaveVariation = false;
                long regionSeed = 0L;
                this.caveRNG.setSeed(((long)(type / 16) * 341873128712L + (long)(range / 16) * 132897987541L) * this.seedMultiplier);
                this.largeCaveRNG.setSeed(this.rand.nextLong());

                if (!genNormalCaves || this.caveRNG.nextInt(4) != 0)
                {
                    i = (1 << this.caveRNG.nextInt(3)) - 1;
                    applyCaveVariation = true;
                    regionSeed = this.caveRNG.nextLong();
                }

                int circularRoomChance;

                if (genCaves < 5)
                {
                    if ((type & 7) == 0 && (range & 7) == 0 && (type & 8) == (range & 8))
                    {
                        this.generateLargeCave(chunkX, chunkZ, 1);
                    }
                    else if (caveSize <= 3 && this.largeCaveRNG.nextInt(4) <= i && this.validLargeCaveLocation(chunkX, chunkZ, caveSize))
                    {
                        int largerCircularRooms = 1;

                        if (this.largeCaveRNG.nextInt(10) == 0)
                        {
                            largerCircularRooms += this.largeCaveRNG.nextInt(4);
                        }

                        for (circularRoomChance = 0; circularRoomChance < largerCircularRooms; ++circularRoomChance)
                        {
                            this.generateLargeCave(chunkX, chunkZ, 0);
                        }
                    }
                }

                if (flag <= 40)
                {
                    boolean var27 = false;
                    circularRoomChance = 4;
                    boolean largerLargeCaves = false;
                    i = 10;
                    float width = 1.0F;
                    float curviness = 0.1F;
                    boolean lavaLevelCaves = false;
                    boolean seaLevelCaves = false;

                    if (applyCaveVariation)
                    {
                        this.caveRNG.setSeed(regionSeed);

                        if (caveSize < 20)
                        {
                            var27 = this.caveRNG.nextBoolean();
                            circularRoomChance = 2 << this.caveRNG.nextInt(2) + this.caveRNG.nextInt(2);
                            largerLargeCaves = this.caveRNG.nextBoolean();
                            i = 5 << this.caveRNG.nextInt(2) + this.caveRNG.nextInt(2);
                        }

                        if (this.caveRNG.nextBoolean())
                        {
                            width += this.caveRNG.nextFloat();

                            if (this.caveRNG.nextBoolean())
                            {
                                width /= 2.0F;
                            }
                        }

                        if (this.caveRNG.nextBoolean())
                        {
                            if (this.caveRNG.nextBoolean())
                            {
                                curviness /= 2.0F;
                            }

                            if (this.rand.nextBoolean())
                            {
                                curviness += this.rand.nextFloat() * curviness;
                            }
                            else
                            {
                                curviness += this.caveRNG.nextFloat() * curviness;
                            }
                        }

                        if (caveSize >= 20)
                        {
                            float caveSizeSL = (float)(caveSize / 10);
                            width = (width + caveSizeSL - 1.0F) / caveSizeSL;
                            curviness = (curviness + (caveSizeSL - 1.0F) / 10.0F) / caveSizeSL;
                        }

                        lavaLevelCaves = this.caveRNG.nextBoolean();
                        seaLevelCaves = this.caveRNG.nextBoolean();
                    }

                    int var28;

                    if (genNormalCaves)
                    {
                        var28 = caveSize;

                        if (caveSize >= 10)
                        {
                            if (width > 1.5F)
                            {
                                var28 = caveSize / 2 + 1;
                            }
                            else if (width > 1.25F)
                            {
                                var28 = caveSize * 3 / 4 + 1;
                            }
                        }

                        if (genCaves == 5 && var28 > 2)
                        {
                            var28 = var28 / 4 + 2;
                        }

                        this.generateCaveSystem(var28, blockX, blockZ, width, curviness, largerLargeCaves, i, var27, circularRoomChance);
                    }

                    caveSize /= 5;

                    if (caveSize > 0)
                    {
                        if (caveSize < 4)
                        {
                            int range1;
                            int i1;

                            if (lavaLevelCaves)
                            {
                                var28 = this.rand.nextInt(this.rand.nextInt(caveSize + 3) + 1);

                                if (var28 > 2)
                                {
                                    var28 = 2;
                                }

                                for (range1 = 0; range1 < var28; ++range1)
                                {
                                    i1 = 1;

                                    if (this.rand.nextInt(4) == 0)
                                    {
                                        i1 += this.rand.nextInt(4);
                                    }

                                    for (int LL = 0; LL < i1; ++LL)
                                    {
                                        this.generateSingleCave(blockX, 3, blockZ, curviness);
                                    }
                                }
                            }

                            if (seaLevelCaves)
                            {
                                var28 = this.rand.nextInt(caveSize * 2 + 1);

                                if (var28 > caveSize)
                                {
                                    var28 = caveSize;
                                }

                                range1 = 23 - var28 * 5;

                                for (i1 = 0; i1 < var28; ++i1)
                                {
                                    this.generateSingleCave(blockX, this.rand.nextInt(range1) + i1 * 5 + 40, blockZ, curviness);
                                }
                            }
                        }
                        else
                        {
                            caveSize /= 2;
                        }
                    }
                    else
                    {
                        caveSize = this.rand.nextInt(2);
                    }
                }
            }
            else if (flag <= 65 && genCaves == 1)
            {
                genCaves = this.validCaveClusterLocation(chunkX, chunkZ, true);

                if (genCaves > 0)
                {
                    this.generateCaveCluster(chunkX, chunkZ, genCaves == 2);
                }
            }
        }

        if (chance <= 1 && flag <= 40)
        {
            if (chance == 1)
            {
                caveSize = this.rand.nextInt(this.rand.nextInt(this.rand.nextInt(40) + 1) + 1) / 5;

                if (caveSize > 3)
                {
                    caveSize /= 2;
                }
            }

            if (caveSize > 0)
            {
                blockX = chunkX * 16;
                blockZ = chunkZ * 16;
                biomebase = this.world.getWorldChunkManager().getBiome(blockX + 8, blockZ + 8);

                if (biomebase == BiomeBase.RAINFOREST || biomebase == BiomeBase.SEASONAL_FOREST || biomebase == BiomeBase.FOREST || biomebase == BiomeBase.TAIGA || biomebase == BiomeBase.PLAINS) {
                    caveSize += this.rand.nextInt(caveSize * 2 + 2);
                } else if (biomebase == BiomeBase.SWAMPLAND || biomebase == BiomeBase.SAVANNA || biomebase == BiomeBase.SHRUBLAND) {
                    caveSize += this.rand.nextInt(caveSize + this.rand.nextInt(2) + 1);
                } else if (biomebase == BiomeBase.DESERT || biomebase == BiomeBase.TUNDRA) {
                    caveSize = caveSize / 2 + this.rand.nextInt(caveSize + this.rand.nextInt(2) + 1);
                } else {
                    caveSize = 0;
                }

                if (caveSize > 0)
                {
                    if (caveSize > 9)
                    {
                        caveSize = 9;
                    }

                    range = 50 - caveSize * 5;
                    chance = 50 + chance * 10;

                    for (i = 0; i < caveSize; ++i)
                    {
                        this.generateSingleCave(blockX, this.rand.nextInt(range) + chance + i * 5, blockZ, 0.1F);
                    }
                }
            }
        }
    }

    public boolean validLargeCaveLocation(int chunkX, int chunkZ, int caves)
    {
        for (int x = -2; x <= 2; ++x)
        {
            for (int z = -2; z <= 2; ++z)
            {
                int x2z2 = x * x + z * z;

                if (x2z2 > 0 && x2z2 <= 5)
                {
                    this.caveRNG.setSeed(((long)(chunkX + x) * 341873128712L + (long)(chunkZ + z) * 132897987541L) * this.seedMultiplier);

                    if (this.caveRNG.nextInt(15) == 0)
                    {
                        caves += this.caveRNG.nextInt(this.caveRNG.nextInt(this.caveRNG.nextInt(40) + 1) + 1);

                        if (caves > 12)
                        {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    private void generateCaveCluster(int centerX, int centerZ, boolean comboCave)
    {
        centerX = centerX * 16 + 8;
        centerZ = centerZ * 16 + 8;
        int type = comboCave ? this.rand.nextInt(3) : this.rand.nextInt(4);
        int quadrant = this.rand.nextInt(4);
        double y = 3.0D;
        int size = this.rand.nextInt(6) + 24;
        double yInc = 53.0D / (double)(size - 1);
        int mazeIndex = comboCave ? this.rand.nextInt(size) : -1;
        int centerOffset = comboCave ? 8 : 4;

        for (int c = comboCave ? size : 1; c > 0; --c)
        {
            int x;
            int y2;
            int z;
            int i;
            int length;
            int dirSwitch;
            double var28;
            double var29;
            double var31;
            label209:

            switch (type)
            {
                case 0:
                    if (!comboCave)
                    {
                        y = (double)this.rand.nextInt(35);
                    }

                    x = comboCave ? 1 : this.rand.nextInt(4) + 3;

                    while (true)
                    {
                        if (x <= 0)
                        {
                            break label209;
                        }

                        var28 = (double)(centerX + (centerOffset + this.rand.nextInt(5)) * this.getQuadrantX(x + quadrant));
                        var29 = comboCave ? y + (double)(this.rand.nextFloat() * 2.0F - 1.0F) : y;
                        var31 = (double)(centerZ + (centerOffset + this.rand.nextInt(5)) * this.getQuadrantZ(x + quadrant));
                        this.generateCircularRoom(var28, var29, var31, this.rand.nextFloat() * this.rand.nextFloat() * 6.0F + 3.0F);
                        this.generateDirectionalCave((int)(var28 + 0.5D), (int)(var29 + 0.5D), (int)(var31 + 0.5D), centerX, centerZ, 0);

                        if (!comboCave)
                        {
                            y += (double)(this.rand.nextInt(4) + 2);
                        }

                        --x;
                    }

                case 1:
                    if (!comboCave)
                    {
                        y = (double)this.rand.nextInt(35);
                    }

                    x = comboCave ? 1 : this.rand.nextInt(4) + 3;

                    while (true)
                    {
                        if (x <= 0)
                        {
                            break label209;
                        }

                        var28 = (double)(centerX + (centerOffset + this.rand.nextInt(5)) * this.getQuadrantX(x + quadrant));
                        var29 = comboCave ? y + (double)(this.rand.nextFloat() * 2.0F - 1.0F) : y;
                        var31 = (double)(centerZ + (centerOffset + this.rand.nextInt(5)) * this.getQuadrantZ(x + quadrant));
                        this.generateRavineCave(var28, var29, var31, comboCave ? 2.0F : 1.0F);

                        if (!comboCave)
                        {
                            y += (double)(this.rand.nextInt(3) + 3);
                        }

                        --x;
                    }

                case 2:
                    if (!comboCave)
                    {
                        y = (double)this.rand.nextInt(8);
                    }

                    x = comboCave ? 1 : this.rand.nextInt(4) + 3;

                    while (true)
                    {
                        if (x <= 0)
                        {
                            break label209;
                        }

                        var28 = (double)(centerX + (centerOffset + this.rand.nextInt(5)) * this.getQuadrantX(x + quadrant));
                        i = Math.round(comboCave ? ((float)y + this.rand.nextFloat() * 2.0F - 1.0F) / 1.5F : (float)y);
                        double var30 = (double)(centerZ + (centerOffset + this.rand.nextInt(5)) * this.getQuadrantZ(x + quadrant));
                        length = i - this.rand.nextInt(5);
                        dirSwitch = i + Math.min(40 + this.rand.nextInt(16), 24 + this.rand.nextInt(36 - i / 2));
                        float var32 = this.rand.nextFloat() * this.rand.nextFloat() * 2.0F;

                        if (this.rand.nextInt(10) == 0)
                        {
                            var32 = Math.min(8.0F, var32 * (this.rand.nextFloat() * 3.0F + 1.0F) + 2.0F);
                        }

                        if ((x + c & 1) == 0)
                        {
                            this.generateVerticalCave(this.rand.nextLong(), var28, length, dirSwitch, var30, var32, (double)centerX, (double)centerZ, comboCave ? 24 : 16);
                        }
                        else
                        {
                            this.generateVerticalCave(this.rand.nextLong(), var28, dirSwitch - length, 0, var30, var32, (double)centerX, (double)centerZ, comboCave ? 24 : 16);
                        }

                        if (!comboCave)
                        {
                            y += (double)(this.rand.nextInt(4) + 3);
                        }

                        --x;
                    }

                case 3:
                    x = this.rand.nextInt(8);
                    y2 = centerX + (4 + this.rand.nextInt(5)) * this.getQuadrantX(1 + quadrant);
                    z = comboCave ? Math.max(3, Math.round((float)y)) : this.rand.nextInt(50) + 3;
                    i = centerZ + (4 + this.rand.nextInt(5)) * this.getQuadrantZ(1 + quadrant);
                    float height = z == 3 ? 2.625F : 1.625F;

                    for (int d = 0; d < 4; ++d)
                    {
                        x += 2;
                        length = this.rand.nextInt(comboCave ? 16 : 8);

                        if ((x & 1) == 0)
                        {
                            length += comboCave ? 28 : 24;
                        }
                        else
                        {
                            length += comboCave ? 20 : 17;
                        }

                        this.generateMazeCaveSegment(this.rand.nextLong(), y2, z, i, x, length, height);
                        dirSwitch = this.rand.nextInt(2) * 4 + 2;
                        int maxOffset = length * 3 / 4;

                        for (int offset = length / 5 + this.rand.nextInt(length / 4); offset < maxOffset; offset += length / 6 + this.rand.nextInt(length / 4) + 1)
                        {
                            dirSwitch += 4;
                            int x2 = this.getOffsetX(y2, x, offset);
                            int z2 = this.getOffsetZ(i, x, offset);
                            int direction2 = x + dirSwitch;
                            int length2 = length / 3 + this.rand.nextInt(length / 3);

                            if ((direction2 & 1) == 1)
                            {
                                length2 -= offset / 4;
                            }

                            this.generateMazeCaveSegment(this.rand.nextLong(), x2, z, z2, direction2, length2, height);

                            if (offset > length / 2)
                            {
                                offset += this.rand.nextInt(length / 6) + 2;
                                x2 = this.getOffsetX(y2, x, offset);
                                z2 = this.getOffsetZ(i, x, offset);
                                direction2 += 4;
                                length2 = length / 3 + this.rand.nextInt(length / 3);

                                if ((direction2 & 1) == 1)
                                {
                                    length2 -= offset / 4;
                                }

                                this.generateMazeCaveSegment(this.rand.nextLong(), x2, z, z2, direction2, length2, height);
                            }
                        }
                    }
            }

            ++quadrant;
            y += yInc;

            if (c == mazeIndex)
            {
                type = 3;
                x = centerX + (centerOffset + this.rand.nextInt(5)) * this.getQuadrantX(quadrant);
                y2 = this.rand.nextInt(16) + 16;
                z = centerZ + (centerOffset + this.rand.nextInt(5)) * this.getQuadrantZ(quadrant);
                this.generateVerticalCave(this.rand.nextLong(), (double)x, y2, 100, (double)z, this.rand.nextFloat(), (double)centerX, (double)centerZ, 24);
                this.generateHorizontalLinkCave(x, y2, z, centerX, centerZ);
                this.generateCircularRoom((double)x, (double)y2, (double)z, this.rand.nextFloat() * this.rand.nextFloat() * 6.0F + 3.0F);
                ++quadrant;
                x = centerX + (centerOffset + this.rand.nextInt(5)) * this.getQuadrantX(quadrant);
                y2 = this.rand.nextInt(16) + 32;
                z = centerZ + (centerOffset + this.rand.nextInt(5)) * this.getQuadrantZ(quadrant);
                this.generateVerticalCave(this.rand.nextLong(), (double)x, y2, 100, (double)z, this.rand.nextFloat(), (double)centerX, (double)centerZ, 24);
                ++quadrant;

                if (mazeIndex > 0)
                {
                    for (i = this.rand.nextInt(3) + 4; i > 0; --i)
                    {
                        x = centerX + (centerOffset + 8 + this.rand.nextInt(16)) * this.getQuadrantX(quadrant);
                        z = centerZ + (centerOffset + 8 + this.rand.nextInt(16)) * this.getQuadrantZ(quadrant);
                        this.generateDirectionalCave(x, 3, z, centerX, centerZ, 0);
                        ++quadrant;
                    }
                }
            }
            else
            {
                type = (type + 1) % 3;
            }
        }
    }

    private int validCaveClusterLocation(int chunkX, int chunkZ, boolean firstCheck)
    {
        if (!firstCheck)
        {
            this.caveRNG.setSeed(((long)chunkX * 341873128712L + (long)chunkZ * 132897987541L) * this.seedMultiplier);

            if (this.caveRNG.nextInt(15) != 0 || this.caveRNG.nextInt(this.caveRNG.nextInt(this.caveRNG.nextInt(40) + 1) + 1) != 0)
            {
                return 0;
            }
        }

        byte flag = 2;
        int x;
        int z;
        int x2z2;

        for (x = -3; x <= 3; ++x)
        {
            for (z = -3; z <= 3; ++z)
            {
                x2z2 = x * x + z * z;

                if (x2z2 <= 13)
                {
                    int chunkOffX = chunkX + x;
                    int chunkOffZ = chunkZ + z;

                    if (x2z2 != 0)
                    {
                        this.caveRNG.setSeed(((long)chunkOffX * 341873128712L + (long)chunkOffZ * 132897987541L) * this.seedMultiplier);

                        if (this.caveRNG.nextInt(15) == 0 && this.caveRNG.nextInt(this.caveRNG.nextInt(this.caveRNG.nextInt(40) + 1) + 1) != 0)
                        {
                            if (!firstCheck || x2z2 <= 5)
                            {
                                return 0;
                            }

                            flag = 1;
                        }
                    }

                    this.caveRNG.setSeed(((long)chunkOffX * 341873128712L + (long)chunkOffZ * 132897987541L) * this.seedMultiplier);
                    int chunkModX;
                    int chunkModZ;

                    if (this.caveRNG.nextInt(20) == 15)
                    {
                        chunkModX = chunkOffX + this.caveOffsetX + 4;
                        chunkModZ = chunkOffZ + this.caveOffsetZ + 4;
                        boolean ravine = false;

                        if ((chunkModX & 7) == 0 && (chunkModZ & 7) == 0 && (chunkModX & 8) != (chunkModZ & 8))
                        {
                            ravine = true;
                        }
                        else if (this.caveRNG.nextInt(25) < 19 && chunkModX % 3 == 0 && chunkModZ % 3 == 0 && (chunkModX / 3 & 1) == (chunkModZ / 3 & 1))
                        {
                            ravine = true;
                        }

                        if (ravine || this.caveRNG.nextInt(30) < 11)
                        {
                            if (!firstCheck || x2z2 <= 5)
                            {
                                return 0;
                            }

                            flag = 1;
                        }
                    }

                    if ((chunkOffX / 7 & 1) != (chunkOffZ / 7 & 1))
                    {
                        chunkModX = chunkOffX % 7;
                        chunkModZ = chunkOffZ % 7;

                        if (chunkModX <= 2 && chunkModZ <= 2)
                        {
                            this.caveRNG.setSeed(((long)(chunkOffX / 7) * 341873128712L + (long)(chunkOffZ / 7) * 132897987541L) * this.seedMultiplier);

                            if (chunkModX == this.caveRNG.nextInt(3) && chunkModZ == this.caveRNG.nextInt(3))
                            {
                                if (!firstCheck || x2z2 <= 5)
                                {
                                    return 0;
                                }

                                flag = 1;
                            }
                        }
                    }
                }
            }
        }

        if (flag == 2 && firstCheck)
        {
            for (x = -3; x <= 3; ++x)
            {
                for (z = -3; z <= 3; ++z)
                {
                    x2z2 = x * x + z * z;

                    if (x2z2 > 0 && x2z2 <= 10 && this.validCaveClusterLocation(chunkX + x, chunkZ + z, false) == 2)
                    {
                        return 1;
                    }
                }
            }
        }

        return flag;
    }

    private void generateCaveSystem(int size, int centerX, int centerZ, float widthMultiplier, float curviness, boolean largerLargeCaves, int largeCaveChance, boolean largerCircularRooms, int circularRoomChance)
    {
        byte spread = 16;

        if (curviness >= 0.15F)
        {
            spread = 32;
            centerX -= 8;
            centerZ -= 8;
        }

        for (int i = 0; i < size; ++i)
        {
            double x = (double)(centerX + this.rand.nextInt(spread));
            double y = (double)(this.rand.nextInt(this.rand.nextInt(120) + 8) - 7);
            double z = (double)(centerZ + this.rand.nextInt(spread));
            int caves = 1;
            float startDirection;

            if (this.rand.nextInt(circularRoomChance) == 0)
            {
                caves += this.rand.nextInt(4);
                startDirection = this.rand.nextFloat() * 6.0F + 1.0F;

                if (largerCircularRooms && this.rand.nextInt(16 / circularRoomChance) == 0)
                {
                    startDirection = startDirection * (this.rand.nextFloat() * this.rand.nextFloat() + 1.0F) + 3.0F;

                    if (startDirection > 7.0F)
                    {
                        caves += 2;

                        if (y < -0.5D)
                        {
                            y += 7.0D;
                        }
                        else if (y > 63.5D)
                        {
                            y -= 60.0D;
                        }
                    }
                }

                if (widthMultiplier < 1.0F)
                {
                    if (startDirection > 8.5F)
                    {
                        startDirection *= widthMultiplier;
                    }
                }
                else
                {
                    startDirection *= widthMultiplier;

                    if (startDirection > 23.5F)
                    {
                        startDirection = 23.5F;
                    }
                }

                this.generateCircularRoom(x, y, z, startDirection);
            }

            startDirection = this.rand.nextFloat() * ((float)Math.PI * 2F);

            for (int j = 0; j < caves; ++j)
            {
                float width = this.rand.nextFloat() * 2.0F + this.rand.nextFloat();

                if (this.rand.nextInt(largeCaveChance) == 0)
                {
                    width *= this.rand.nextFloat() * this.rand.nextFloat() * 4.0F + 1.0F;

                    if (largerLargeCaves)
                    {
                        if (widthMultiplier < 1.0F)
                        {
                            if (width > 7.5F)
                            {
                                width *= widthMultiplier;
                            }
                        }
                        else if (width < 7.5F)
                        {
                            width *= widthMultiplier;
                        }
                    }
                    else if (width > 8.5F)
                    {
                        width = 8.5F;
                    }
                }
                else
                {
                    width *= widthMultiplier;
                }

                float direction = startDirection;

                if (j > 0)
                {
                    direction = startDirection + ((float)Math.PI * 2F) * (float)j / (float)caves + (this.rand.nextFloat() - 0.5F) * ((float)Math.PI * 2F) / (float)caves;
                }

                this.generateCave(this.rand.nextLong(), x, y, z, width, direction, (this.rand.nextFloat() - 0.5F) / 4.0F, 0, 0, curviness);
            }
        }
    }

    private void generateColossalCaveSystem(int centerX, int centerZ)
    {
        centerX *= 16;
        centerZ *= 16;
        int caveType = this.rand.nextInt(5);
        int caveCounter = this.rand.nextInt(caveType == 4 ? 192 : 200);
        int sizeOffset = this.rand.nextInt(2);
        int i;
        int z;
        int x2z2;
        int subCenterX;
        int subCenterZ;
        int size;

        if (caveType < 4)
        {
            z = this.rand.nextInt(3) - 1;
            boolean x = false;

            for (x2z2 = 0; x2z2 < 8; ++x2z2)
            {
                subCenterX = centerX;
                subCenterZ = centerZ;

                do
                {
                    do
                    {
                        i = this.rand.nextInt(3) - 1;
                    }
                    while (x && i == z);
                }
                while (Math.abs(i - z) > 1);

                x = i == z;
                z = i;

                switch (caveType)
                {
                    case 0:
                        subCenterX = centerX + i * 16;
                        subCenterZ = centerZ + (x2z2 * 16 - 56);
                        break;

                    case 1:
                        subCenterX = centerX + (x2z2 * 16 - 56);
                        subCenterZ = centerZ + i * 16;
                        break;

                    case 2:
                        subCenterX = centerX + x2z2 * 23 / 2 - 40 + i * 8;
                        subCenterZ = centerZ + (x2z2 * 13 - 45);
                        break;

                    case 3:
                        subCenterX = centerX + 40 - x2z2 * 23 / 2 + i * 8;
                        subCenterZ = centerZ + (x2z2 * 13 - 45);
                }

                if (caveType < 2)
                {
                    caveCounter = this.generateSubCaveSystem(25, subCenterX, subCenterZ, caveCounter);
                }
                else
                {
                    size = (x2z2 + sizeOffset & 1) + 12;
                    caveCounter = this.generateSubCaveSystem(size, subCenterX - 8, subCenterZ, caveCounter);
                    caveCounter = this.generateSubCaveSystem(25 - size, subCenterX + 8, subCenterZ, caveCounter);
                }
            }
        }
        else
        {
            i = 0;

            for (z = -2; z <= 2; ++z)
            {
                for (int var13 = -2; var13 <= 2; ++var13)
                {
                    x2z2 = var13 * var13 + z * z;

                    if (x2z2 > 0 && x2z2 <= 5)
                    {
                        subCenterX = centerX + var13 * 16;
                        subCenterZ = centerZ + z * 16;
                        size = 9 + (i & 1);
                        caveCounter = this.generateSubCaveSystem(size, subCenterX, subCenterZ, caveCounter);
                    }

                    ++i;
                }
            }
        }
    }

    private int generateSubCaveSystem(int size, int centerX, int centerZ, int caveCounter)
    {
        for (int i = 0; i < size; ++i)
        {
            double x = (double)(centerX + this.rand.nextInt(16));
            double z = (double)(centerZ + this.rand.nextInt(16));
            int index12 = caveCounter % 12;
            double y = -7.0D;

            if (index12 < 9)
            {
                int index3 = caveCounter % 3;

                if (index3 < 2)
                {
                    y += (double)(index3 * 10 + this.rand.nextInt(10));
                }
                else
                {
                    y += (double)(index12 * 3 + 14 + this.rand.nextInt(9));
                }
            }
            else if (index12 == 9)
            {
                y += (double)(47 + this.rand.nextInt(11));
            }
            else if (index12 == 10)
            {
                y += (double)(58 + this.rand.nextInt(13));
            }
            else
            {
                y += (double)(71 + this.rand.nextInt(20));
            }

            if (caveCounter % 7 == 0)
            {
                this.generateCircularRoom(x, y, z, this.rand.nextFloat() * 5.0F + 2.0F);
            }

            this.generateCave(this.rand.nextLong(), x, y, z, this.rand.nextFloat() * 2.0F + this.rand.nextFloat(), this.rand.nextFloat() * ((float)Math.PI * 2F), (this.rand.nextFloat() - 0.5F) / 4.0F, 0, 0, 0.1F);
            ++caveCounter;
        }

        return caveCounter;
    }

    private void generateSpecialCaveSystems(int chunkX, int chunkZ, int type)
    {
        int centerX = chunkX * 16 + 8;
        int centerZ = chunkZ * 16 + 8;

        if (type == 2)
        {
            if ((chunkX + this.caveOffsetX - 15 & 32) == (chunkZ + this.caveOffsetZ - 15 & 32))
            {
                this.generateCircularRoomCaveSystem(centerX, centerZ);
            }
            else
            {
                this.generateRavineCaveSystem(centerX, centerZ);
            }
        }
        else if (type == 6)
        {
            this.generateVerticalCaveSystem(centerX, centerZ);
        }
        else
        {
            this.generateMazeCaveSystem(centerX, centerZ);
        }
    }

    private void generateCircularRoomCaveSystem(int centerX, int centerZ)
    {
        int caveSize = this.rand.nextInt(15) + 35;
        double yInc = 39.0D / (double)caveSize;
        double y = 0.0D;
        int offset = this.rand.nextInt(4);
        int centerCave = this.rand.nextInt(2);

        for (int i = 0; i < caveSize; ++i)
        {
            int x;
            int z;
            int x2z2;

            do
            {
                x = this.rand.nextInt(33);
                z = this.rand.nextInt(33);
                x2z2 = x * x + z * z;
            }
            while (x2z2 < 101 || x2z2 > 1025);

            x *= this.getQuadrantX(i + offset);
            z *= this.getQuadrantZ(i + offset);
            x += centerX;
            y += ((double)i / (double)(caveSize - 1) + 1.0D) * yInc;
            z += centerZ;
            this.generateCircularRoom((double)x, y, (double)z, this.rand.nextFloat() * this.rand.nextFloat() * 9.0F + 3.0F);
            this.generateDirectionalCave(x, (int)(y + 0.5D), z, centerX, centerZ, 16);

            if (i < 2)
            {
                x = centerX + (this.rand.nextInt(9) + 8) * (this.rand.nextInt(2) * 2 - 1);
                int y2 = this.rand.nextInt(8) + i * 16 + 16;
                z = centerZ + (this.rand.nextInt(9) + 8) * (this.rand.nextInt(2) * 2 - 1);
                this.generateVerticalCave((double)x, y2, 100, (double)z);
                this.generateCircularRoom((double)x, (double)y2, (double)z, this.rand.nextFloat() * this.rand.nextFloat() * 9.0F + 3.0F);
                this.generateDirectionalCave(x, y2, z, centerX, centerZ, 999);
                this.generateHorizontalLinkCave(x, y2, z, centerX, centerZ);
            }

            if ((i & 7) == centerCave)
            {
                x = centerX + (this.rand.nextInt(6) + 3) * (this.rand.nextInt(2) * 2 - 1);
                double var16 = y + (double)(this.rand.nextInt(9) - 4);

                if (var16 < 0.0D)
                {
                    var16 += 4.0D;
                }

                z = centerZ + (this.rand.nextInt(6) + 3) * (this.rand.nextInt(2) * 2 - 1);
                this.generateCircularRoom((double)x, y, (double)z, this.rand.nextFloat() * this.rand.nextFloat() * 9.0F + 3.0F);

                if (i == centerCave)
                {
                    this.generateVerticalCave((double)x, 3, 32, (double)z);
                }
            }
        }
    }

    private void generateRavineCaveSystem(int centerX, int centerZ)
    {
        int caveSize = this.rand.nextInt(10) + 30;
        double yInc = 39.0D / (double)caveSize;
        double y = 0.0D;
        int offset = this.rand.nextInt(4);
        int vertCave1 = caveSize / 4 + this.rand.nextInt(3);
        int vertCave2 = caveSize / 3 + this.rand.nextInt(3);
        int centerCave = this.rand.nextInt(3);

        for (int i = 0; i < caveSize; ++i)
        {
            int x;
            int z;
            int x2z2;

            do
            {
                x = this.rand.nextInt(33);
                z = this.rand.nextInt(33);
                x2z2 = x * x + z * z;
            }
            while (x2z2 < 145 || x2z2 > 1025);

            x *= this.getQuadrantX(i + offset);
            z *= this.getQuadrantZ(i + offset);
            x += centerX;
            y += ((double)i / (double)(caveSize - 1) + 1.0D) * yInc;
            z += centerZ;
            this.generateRavineCave((double)x, y, (double)z, 2.0F);

            if (i == vertCave1 || i == vertCave2)
            {
                this.generateVerticalCave((double)x, (int)(y + 0.5D), 100, (double)z);
                this.generateHorizontalLinkCave(x, (int)(y + 0.5D), z, centerX, centerZ);
            }

            if (i % 7 == centerCave)
            {
                x = centerX + (this.rand.nextInt(6) + 3) * (this.rand.nextInt(2) * 2 - 1);
                double y2 = y + (double)(this.rand.nextInt(5) - 2);

                if (y2 < 0.0D)
                {
                    y2 += 4.0D;
                }

                z = centerZ + (this.rand.nextInt(6) + 3) * (this.rand.nextInt(2) * 2 - 1);
                int length = this.rand.nextInt(17) + 30;
                int segmentLength = length + this.rand.nextInt(2) * 8 - 4;
                float width = this.rand.nextFloat() * this.rand.nextFloat() + 1.0F;
                float height = this.rand.nextFloat() * 2.0F + 2.0F;
                float direction = this.rand.nextFloat() * ((float)Math.PI / 4F) + (float)i * 0.112199F;
                float directionY = (this.rand.nextFloat() - 0.5F) / 4.0F;
                float slope = (this.rand.nextFloat() * 0.75F + 0.25F) * 0.25F * (float)(this.rand.nextInt(2) * 2 - 1);
                this.generateRavineCaveSegment(this.rand.nextLong(), (double)x, y, (double)z, width, direction, directionY, slope, segmentLength, height);
                segmentLength = length + (this.rand.nextInt(2) * 2 - 1) * 8 - 4;
                this.generateRavineCaveSegment(this.rand.nextLong(), (double)x, y, (double)z, width, direction + (float)Math.PI, -directionY, -slope, segmentLength, height);
            }
        }
    }

    private void generateVerticalCaveSystem(int centerX, int centerZ)
    {
        int caveSize = this.rand.nextInt(15) + 45;
        double yInc = 39.0D / (double)caveSize;
        double y = 0.0D;
        int offset = this.rand.nextInt(4);
        int horizCave = this.rand.nextInt(3);
        int deepHorizCave = 6 + this.rand.nextInt(3);
        int vertCave1 = caveSize / 3 + (this.rand.nextInt(5) - 2) * 2;
        int vertCave2 = caveSize / 3 + (this.rand.nextInt(5) - 2) * 2 + this.rand.nextInt(2) * 2 - 1;
        int largeCaveChance = (caveSize - 29) / 5;
        largeCaveChance += this.rand.nextInt(8 - largeCaveChance) + 5;
        int largeCaveOffset = this.rand.nextInt(largeCaveChance);

        for (int i = 0; i < caveSize; ++i)
        {
            int x;
            int z;
            int x2z2;

            do
            {
                x = this.rand.nextInt(33);
                z = this.rand.nextInt(33);
                x2z2 = x * x + z * z;
            }
            while (x2z2 < 101 || x2z2 > 1025);

            x *= this.getQuadrantX(i + offset);
            z *= this.getQuadrantZ(i + offset);
            y += ((double)i / (double)(caveSize - 1) + 1.0D) * yInc;
            int y2 = (int)y;

            if (i < deepHorizCave)
            {
                this.generateDirectionalCave(centerX - x - (x >= 0 ? 8 : -8), 3, centerZ - z - (z >= 0 ? 8 : -8), centerX, centerZ, 0);
            }

            if (i % 3 == horizCave)
            {
                this.generateDirectionalCave(centerX + x + (x >= 0 ? 8 : -8), y2, centerZ + z + (z >= 0 ? 8 : -8), centerX, centerZ, 0);
            }

            if (i == vertCave1 || i == vertCave2)
            {
                this.generateHorizontalLinkCave(centerX - x, y2, centerZ - z, centerX, centerZ);
            }

            y2 /= 3;
            int minY = y2 - this.rand.nextInt(5);
            int maxY = y2 + 32 + this.rand.nextInt(33 - y2);
            float width = this.rand.nextFloat() * this.rand.nextFloat() * 2.0F;

            if (i % largeCaveChance == largeCaveOffset)
            {
                width = Math.min(8.0F, width * (this.rand.nextFloat() * 3.0F + 1.0F) + 2.0F);
            }

            x += centerX;
            z += centerZ;

            if ((i + offset & 4) == 0)
            {
                this.generateVerticalCave(this.rand.nextLong(), (double)x, minY, maxY, (double)z, width, (double)centerX, (double)centerZ, 40);
            }
            else
            {
                this.generateVerticalCave(this.rand.nextLong(), (double)x, maxY - minY, 0, (double)z, width, (double)centerX, (double)centerZ, 40);
            }

            if (i == vertCave1 || i == vertCave2)
            {
                x = centerX + (this.rand.nextInt(6) + 3) * (this.rand.nextInt(2) * 2 - 1);
                z = centerZ + (this.rand.nextInt(6) + 3) * (this.rand.nextInt(2) * 2 - 1);

                if (i == vertCave1)
                {
                    this.generateVerticalCave((double)x, minY, 100, (double)z);
                }
                else
                {
                    this.generateVerticalCave((double)x, 100, 0, (double)z);
                }
            }
        }
    }

    private void generateMazeCaveSystem(int centerX, int centerZ)
    {
        boolean direction = false;
        int oldDirection = 0;
        boolean change = false;
        int yInc = 7 + this.rand.nextInt(2);
        byte minY = 3;
        byte maxY = 59;
        int quadrant = this.rand.nextInt(4);
        int oldQuadrant = quadrant;
        float height = 2.625F;
        int horizCave1 = this.rand.nextInt(4);
        int horizCave2;

        do
        {
            horizCave2 = this.rand.nextInt(4);
        }
        while (horizCave1 == horizCave2);

        horizCave1 = minY + (horizCave1 + 1) * yInc + this.rand.nextInt(4);
        horizCave2 = minY + (horizCave2 + 1) * yInc + this.rand.nextInt(4);
        int vertCave1 = maxY + this.rand.nextInt(4);
        int vertCave2;

        do
        {
            vertCave2 = maxY + this.rand.nextInt(4);
        }
        while (vertCave1 == vertCave2);

        int caveCount = this.rand.nextInt(2);

        for (int caveY = minY; caveY <= maxY; caveY += yInc)
        {
            int offsetX = this.rand.nextInt(7) + 4;
            int offsetZ = this.rand.nextInt(7) + 4;

            switch (quadrant)
            {
                case 1:
                    offsetX = -offsetX;
                    break;

                case 2:
                    offsetZ = -offsetZ;
                    break;

                case 3:
                    offsetX = -offsetX;
                    offsetZ = -offsetZ;
            }

            int caveX = centerX + offsetX;
            int caveZ = centerZ + offsetZ;
            int var33 = this.rand.nextInt(2);

            if (change && var33 == oldDirection)
            {
                var33 = 1 - var33;
            }

            change = var33 == oldDirection;
            oldDirection = var33;
            var33 += this.rand.nextInt(4) * 2;
            int length;

            for (int d = 0; d < 4; ++d)
            {
                var33 += 2;

                if ((var33 & 1) == 0)
                {
                    length = 28 + this.rand.nextInt(20);
                }
                else
                {
                    length = 20 + this.rand.nextInt(20);
                }

                this.generateMazeCaveSegment(this.rand.nextLong(), caveX, caveY, caveZ, var33, length, height);
                int dirSwitch = this.rand.nextInt(2) * 4 + 2;
                int maxOffset = length * 3 / 4;

                for (int offset = length / 5 + this.rand.nextInt(length / 4); offset < maxOffset; offset += length / 6 + this.rand.nextInt(length / 4) + 1)
                {
                    dirSwitch += 4;
                    int x = this.getOffsetX(caveX, var33, offset);
                    int z = this.getOffsetZ(caveZ, var33, offset);
                    int direction2 = var33 + dirSwitch;
                    int length2 = length / 3 + this.rand.nextInt(length / 3);

                    if ((direction2 & 1) == 1)
                    {
                        length2 -= offset / 4;
                    }

                    this.generateMazeCaveSegment(this.rand.nextLong(), x, caveY, z, direction2, length2, height);
                    int index = caveY + (direction2 / 2 & 3);

                    if (index != horizCave1 && index != horizCave2)
                    {
                        if (index == vertCave1 || index == vertCave2)
                        {
                            int offset2 = length2 / 4 + this.rand.nextInt(length2 / 2 + 1) + 1;
                            x = this.getOffsetX(x, direction2, offset2);
                            z = this.getOffsetZ(z, direction2, offset2);

                            if (index == vertCave1)
                            {
                                vertCave1 = -999;
                                this.generateVerticalCave(this.rand.nextLong(), (double)x, caveY, minY + yInc * 2, (double)z, 0.0F, (double)centerX, (double)centerZ, 32);
                            }
                            else
                            {
                                vertCave2 = -999;
                                this.generateVerticalCave(this.rand.nextLong(), (double)x, caveY, 100, (double)z, 0.0F, 0.0D, 0.0D, 0);
                            }
                        }
                    }
                    else
                    {
                        if (index == horizCave1)
                        {
                            horizCave1 = -999;
                        }
                        else
                        {
                            horizCave2 = -999;
                        }

                        x = this.getOffsetX(x, direction2, length2);
                        z = this.getOffsetZ(z, direction2, length2);
                        this.generateHorizontalLinkCave(x, caveY, z, centerX, centerZ);
                    }

                    if (offset > length / 2)
                    {
                        offset += this.rand.nextInt(length / 6) + 2;
                        x = this.getOffsetX(caveX, var33, offset);
                        z = this.getOffsetZ(caveZ, var33, offset);
                        direction2 += 4;
                        length2 = length / 3 + this.rand.nextInt(length / 3);

                        if ((direction2 & 1) == 1)
                        {
                            length2 -= offset / 4;
                        }

                        this.generateMazeCaveSegment(this.rand.nextLong(), x, caveY, z, direction2, length2, height);
                    }
                }
            }

            if (caveY == maxY)
            {
                length = 100;
            }
            else if (caveY == minY)
            {
                length = maxY - yInc * 2 + 1;
            }
            else
            {
                length = Math.min(maxY, caveY + yInc) + 1;
            }

            this.generateVerticalCave(this.rand.nextLong(), (double)caveX, caveY, length, (double)caveZ, 0.0F, (double)caveX, (double)caveZ, 8);

            do
            {
                quadrant = this.rand.nextInt(4);
            }
            while (quadrant == oldQuadrant);

            oldQuadrant = quadrant;
            height = 1.625F;
            ++caveCount;
        }
    }

    private void generateRegionalCaves(int chunkX, int chunkZ)
    {
        int chunkOffX = chunkX + this.caveOffsetX;
        int chunkOffZ = chunkZ + this.caveOffsetZ;
        double x;

        if (this.isGiantCaveRegion(chunkX, chunkZ))
        {
            this.caveRNG.setSeed(((long)(chunkOffX / 2) * 341873128712L + (long)(chunkOffZ / 2) * 132897987541L) * this.seedMultiplier);

            if ((chunkOffX & 1) == this.caveRNG.nextInt(2) && (chunkOffZ & 1) == this.caveRNG.nextInt(2))
            {
                int div1 = (chunkOffX & 2) == (chunkOffZ & 2) ? 2 : 1;
                this.largeCaveRNG.setSeed(this.rand.nextLong());

                for (int div2 = 0; div2 < div1; ++div2)
                {
                    this.generateLargeCave(chunkX, chunkZ, 2);
                }

                if (this.rand.nextBoolean())
                {
                    double var21 = (double)(chunkX * 16 + 8);
                    int verticalCave = 10 + this.rand.nextInt(20);
                    x = (double)(chunkZ * 16 + 8);
                    this.generateVerticalCave(var21, verticalCave, 100, x);
                    float y = this.rand.nextFloat() * ((float)Math.PI * 2F);

                    for (int i = 0; i < 2; ++i)
                    {
                        float z = this.rand.nextFloat() * this.rand.nextFloat() + 0.5F;
                        int length = 30 + this.rand.nextInt(27);

                        if (i == 1)
                        {
                            y += 2.74889F + this.rand.nextFloat() * ((float)Math.PI / 4F);
                        }

                        this.generateHorizontalCave(this.rand.nextLong(), var21, (double)verticalCave, x, z, y, (this.rand.nextFloat() - 0.5F) / 4.0F, 0, length, 3);
                    }
                }
            }
        }
        else
        {
            byte var20 = 2;
            byte var22 = 3;

            if ((chunkOffX & 64) == (chunkOffZ & 64))
            {
                var20 = 3;
                var22 = 2;
            }

            this.caveRNG.setSeed(((long)(chunkOffX / var20) * 341873128712L + (long)(chunkOffZ / var22) * 132897987541L) * this.seedMultiplier);

            if (chunkOffX % var20 == this.caveRNG.nextInt(var20) && chunkOffZ % var22 == this.caveRNG.nextInt(var22))
            {
                int startY = 10 + (chunkOffX / var22 + chunkOffZ / var20) % 3 * 20;

                boolean var23 = this.rand.nextBoolean();
                x = (double)(chunkX * 16 + this.rand.nextInt(16));
                double var24 = (double)startY;
                double var25 = (double)(chunkZ * 16 + this.rand.nextInt(16));
                float direction = this.rand.nextFloat() * ((float)Math.PI * 2F);
                int segments = this.rand.nextInt(3) + 2;
                float width = !var23 && segments == 2 ? 1.0F : (float)(this.rand.nextInt(2) + 1);
                width = this.rand.nextFloat() * this.rand.nextFloat() * width + 0.5F;

                for (int i1 = 0; i1 < segments; ++i1)
                {
                    float segmentDirection = direction;
                    direction += ((float)Math.PI * 2F) / (float)segments;

                    if (segments > 2)
                    {
                        segmentDirection += (this.rand.nextFloat() - 0.5F) * 2.094395F / (float)segments;
                    }

                    if (i1 > 0 && (var23 || segments > 2))
                    {
                        width = this.rand.nextFloat() * this.rand.nextFloat() * (float)(this.rand.nextInt(2) + 1) + 0.5F;
                    }

                    this.generateHorizontalCave(this.rand.nextLong(), x, var24, var25, width, segmentDirection, (this.rand.nextFloat() - 0.5F) / 4.0F, 0, 112 + this.rand.nextInt(65), 2);
                }

                if (var23)
                {
                    this.generateVerticalCave(x, startY, 100, var25);
                    ++segments;
                }

                if (segments > 2)
                {
                    this.generateCircularRoom(x, var24, var25, (this.rand.nextFloat() + 0.5F) * (float)segments + 1.0F);
                }
            }
        }
    }

    private void generateCave(long seed, double x, double y, double z, float width, float directionXZ, float directionY, int pos, int length, float curviness)
    {
        this.caveRNG.setSeed(seed);
        float var23 = 0.0F;
        float var24 = 0.0F;
        int branchPoint = -999;
        boolean isCircularRoom = false;

        if (pos <= 0)
        {
            if (pos < 0)
            {
                isCircularRoom = true;
                pos = 1;
                length = 2;
            }
            else
            {
                length = 112 - this.caveRNG.nextInt(28);

                if (width >= 1.0F)
                {
                    branchPoint = this.caveRNG.nextInt(length / 2) + length / 4;
                }
            }
        }

        for (boolean isVerticalCave = !isCircularRoom && this.caveRNG.nextInt(6) == 0; pos < length; ++pos)
        {
            if (pos == branchPoint)
            {
                seed = this.caveRNG.nextLong();
                width = this.caveRNG.nextFloat() * 0.5F + 0.5F;
                directionY /= 3.0F;
                this.generateCave(this.caveRNG.nextLong(), x, y, z, this.caveRNG.nextFloat() * 0.5F + 0.5F, directionXZ - ((float)Math.PI / 2F), directionY, pos, length, curviness);
                this.generateCave(seed, x, y, z, width, directionXZ + ((float)Math.PI / 2F), directionY, pos, length, curviness);
                return;
            }

            double var35 = x - this.chunkCenterX;
            double var37 = z - this.chunkCenterZ;
            double radiusW;
            double radiusH;
            double radiusW_2;

            if (isCircularRoom)
            {
                radiusW_2 = (double)(width + 18.0F);

                if (var35 * var35 + var37 * var37 > radiusW_2 * radiusW_2)
                {
                    return;
                }

                radiusW = (double)(width + 1.5F);
                radiusH = radiusW * (double)curviness;
                x += (double)(this.caveRNG.nextFloat() - 0.5F);
                y += (double)(this.caveRNG.nextFloat() - 0.5F);
                z += (double)(this.caveRNG.nextFloat() - 0.5F);
            }
            else
            {
                radiusW = (double)(1.5F + this.sine((float)pos * (float)Math.PI / (float)length) * width);
                radiusW_2 = (double)(length - pos + 18) + radiusW;

                if (var35 * var35 + var37 * var37 > radiusW_2 * radiusW_2)
                {
                    return;
                }

                radiusH = radiusW;

                if (this.caveRNG.nextInt(4) == 0)
                {
                    radiusW = radiusW / 5.0D + 0.75D;
                    radiusH = radiusH / 5.0D + 0.75D;
                }

                float noiseMultiplier = this.cosine(directionY);
                x += (double)(this.cosine(directionXZ) * noiseMultiplier);
                y += (double)this.sine(directionY);
                z += (double)(this.sine(directionXZ) * noiseMultiplier);

                if (isVerticalCave)
                {
                    directionY *= 0.92F;
                }
                else
                {
                    directionY *= 0.7F;
                }

                directionY += var24 * 0.1F;
                directionXZ += var23 * curviness;
                var24 *= 0.9F;
                var23 *= 0.75F;
                var24 += (this.caveRNG.nextFloat() - this.caveRNG.nextFloat()) * this.caveRNG.nextFloat() * 2.0F;
                var23 += (this.caveRNG.nextFloat() - this.caveRNG.nextFloat()) * this.caveRNG.nextFloat() * 4.0F;
            }

            radiusW_2 = radiusW + 9.0D;

            if (x >= this.chunkCenterX - radiusW_2 && x <= this.chunkCenterX + radiusW_2 && z >= this.chunkCenterZ - radiusW_2 && z <= this.chunkCenterZ + radiusW_2)
            {
                double var32 = 0.275D / Math.max(radiusW - 1.0D, 0.915D);

                if (isCircularRoom)
                {
                    var32 *= 1.5D;

                    if (var32 > 0.3D)
                    {
                        var32 = 0.3D;
                    }
                }

                this.generateCaveSegment(x, y, z, radiusW, radiusH, var32, 1);
            }
        }
    }

    private void generateCircularRoom(double x, double y, double z, float width)
    {
        this.generateCave(this.rand.nextLong(), x, y, z, width, 0.0F, 0.0F, -1, -1, 0.5F);
    }

    private void generateSingleCave(int x, int y, int z, float curviness)
    {
        x += this.rand.nextInt(16);
        z += this.rand.nextInt(16);
        float width = this.rand.nextFloat() * this.rand.nextFloat() * this.rand.nextFloat() * 5.0F + 0.5F;
        this.generateCave(this.rand.nextLong(), (double)x, (double)y, (double)z, width, this.rand.nextFloat() * ((float)Math.PI * 2F), (this.rand.nextFloat() - 0.5F) / 4.0F, 0, 0, curviness);
    }

    private void generateLargeCave(int chunkX, int chunkZ, int type)
    {
        int length;
        float width;

        if (type == 1)
        {
            length = 224 + this.largeCaveRNG.nextInt(113);
            width = this.largeCaveRNG.nextFloat() * 12.0F + this.largeCaveRNG.nextFloat() * 6.0F + 6.0F;

            if (this.largeCaveRNG.nextBoolean())
            {
                width *= (float)length / 224.0F;
            }
        }
        else
        {
            length = Math.min(112 + this.largeCaveRNG.nextInt(this.largeCaveRNG.nextInt(336) + 1), 336);

            if (type == 0)
            {
                this.caveRNG.setSeed(((long)((chunkX + this.caveOffsetX + 12) / 16) * 341873128712L + (long)((chunkZ + this.caveOffsetZ + 12) / 16) * 132897987541L) * this.seedMultiplier);
            }

            width = this.largeCaveRNG.nextFloat() * this.largeCaveRNG.nextFloat() * this.largeCaveRNG.nextFloat();

            if (type != 2 && !this.caveRNG.nextBoolean())
            {
                width = width * 2.66667F + 2.66667F;
            }
            else
            {
                width = width * 8.0F + 2.0F;
            }

            if (this.largeCaveRNG.nextBoolean())
            {
                float x = this.largeCaveRNG.nextFloat() * (float)length / 96.0F + (float)(672 - length) / 672.0F;

                if (x > 1.0F)
                {
                    width *= x;
                }
            }
            else
            {
                width *= this.largeCaveRNG.nextFloat() + 1.0F;
            }
        }

        double x1 = (double)(chunkX * 16 + 8);
        double y = (double)(this.largeCaveRNG.nextInt(16) + 15);
        double z = (double)(chunkZ * 16 + 8);

        if (y < 20.5D)
        {
            y += (double)((width + 0.5F) / 4.0F);
        }

        int branchPoint = this.largeCaveRNG.nextInt(length / 4) + length / 2;
        float direction = this.largeCaveRNG.nextFloat() * ((float)Math.PI * 2F);
        float curviness = (float)length / 3360.0F + 0.05F;

        if (type == 1)
        {
            int startPos = length - branchPoint;
            this.generateLargeCave2(this.largeCaveRNG.nextLong(), x1, y, z, width, direction, (this.largeCaveRNG.nextFloat() - 0.5F) / 4.0F, startPos, length, 0, curviness);
            length += startPos;
            startPos = length / 2;
            branchPoint = startPos * 3 / 2 + this.largeCaveRNG.nextInt(startPos / 4);
            this.generateLargeCave2(this.largeCaveRNG.nextLong(), x1, y, z, (this.largeCaveRNG.nextFloat() * width + width) / 3.0F, direction + ((float)Math.PI / 2F), (this.largeCaveRNG.nextFloat() - 0.5F) / 4.0F, startPos, length, branchPoint, curviness);
            this.generateLargeCave2(this.largeCaveRNG.nextLong(), x1, y, z, (this.largeCaveRNG.nextFloat() * width + width) / 3.0F, direction - ((float)Math.PI / 2F), (this.largeCaveRNG.nextFloat() - 0.5F) / 4.0F, startPos, length, branchPoint, curviness);
        }
        else
        {
            this.generateLargeCave2(this.largeCaveRNG.nextLong(), x1, y, z, width, direction, (this.largeCaveRNG.nextFloat() - 0.5F) / 4.0F, 0, length, branchPoint, curviness);
        }
    }

    private void generateLargeCave2(long seed, double x, double y, double z, float width, float directionXZ, float directionY, int pos, int length, int branchPoint, float curviness)
    {
        this.caveRNG.setSeed(seed);
        float var23 = 0.0F;
        float var24 = 0.0F;
        float minRadius = 1.75F + width / 53.3333F;

        for (boolean isVerticalCave = this.caveRNG.nextInt(6) == 0; pos < length; ++pos)
        {
            if (pos == branchPoint)
            {
                seed = this.caveRNG.nextLong();
                width = (this.caveRNG.nextFloat() * width + width) / 3.0F;
                directionY /= 3.0F;
                this.generateLargeCave2(this.caveRNG.nextLong(), x, y, z, (this.caveRNG.nextFloat() * width + width) / 3.0F, directionXZ - ((float)Math.PI / 2F), directionY, pos, length, 0, curviness);
                this.generateLargeCave2(seed, x, y, z, width, directionXZ + ((float)Math.PI / 2F), directionY, pos, length, 0, curviness);
                return;
            }

            double radiusW = (double)(this.sine((float)pos * (float)Math.PI / (float)length) * width);
            double var35 = x - this.chunkCenterX;
            double var37 = z - this.chunkCenterZ;
            double var39 = (double)(length - pos + 18) + radiusW;

            if (var35 * var35 + var37 * var37 > var39 * var39)
            {
                return;
            }

            double ratio = (double)(1.0F - (float)radiusW / 100.0F);
            radiusW += (double)minRadius;
            double radiusH = radiusW * ratio;

            if (this.caveRNG.nextInt(4) == 0)
            {
                radiusW = radiusW / 5.0D + 0.75D;
                radiusH = radiusH / 5.0D + 0.75D;
            }

            float var33 = this.cosine(directionY);
            x += (double)(this.cosine(directionXZ) * var33);
            y += (double)this.sine(directionY);
            z += (double)(this.sine(directionXZ) * var33);

            if (isVerticalCave)
            {
                directionY *= 0.92F;
            }
            else
            {
                directionY *= 0.7F;
            }

            directionY += var24 * 0.1F;
            directionXZ += var23 * curviness;
            var24 *= 0.9F;
            var23 *= 0.75F;
            var24 += (this.caveRNG.nextFloat() - this.caveRNG.nextFloat()) * this.caveRNG.nextFloat() * 2.0F;
            var23 += (this.caveRNG.nextFloat() - this.caveRNG.nextFloat()) * this.caveRNG.nextFloat() * 4.0F;
            double radiusW_2 = radiusW + 9.0D;

            if (x >= this.chunkCenterX - radiusW_2 && x <= this.chunkCenterX + radiusW_2 && z >= this.chunkCenterZ - radiusW_2 && z <= this.chunkCenterZ + radiusW_2)
            {
                double noiseMultiplier = 0.275D / Math.max(radiusW - 1.0D, 0.915D) + 0.0033735D;
                this.generateCaveSegment(x, y, z, radiusW, radiusH, noiseMultiplier, 1);
            }
        }
    }

    private void generateHorizontalCave(long seed, double x, double y, double z, float width, float directionXZ, float directionY, int pos, int length, int caveType)
    {
        this.caveRNG.setSeed(seed);
        float var23 = 0.0F;
        float var24 = 0.0F;
        int branchPoint = -999;
        float startDir = directionXZ;
        double startY = y;
        boolean flag = caveType < 1 || caveType == 2;
        float curviness;

        if (caveType < 2)
        {
            curviness = 0.1F;
        }
        else
        {
            curviness = caveType != 3 && this.caveRNG.nextInt(4) != 0 ? 0.025F : 0.05F;
        }

        if (caveType == 1)
        {
            branchPoint = this.caveRNG.nextInt(length / 4) + length / 2;
        }

        for (boolean isVerticalCave = flag && this.caveRNG.nextInt(6) == 0; pos < length; ++pos)
        {
            if (pos == branchPoint)
            {
                seed = this.caveRNG.nextLong();
                float var371 = Math.min(width * 0.75F, this.caveRNG.nextFloat() * width);
                width = Math.min(width * 0.75F, this.caveRNG.nextFloat() * width);
                directionY /= 3.0F;
                this.generateHorizontalCave(this.caveRNG.nextLong(), x, y, z, width, directionXZ - ((float)Math.PI / 2F), directionY, pos, length, -1);
                this.generateHorizontalCave(seed, x, y, z, var371, directionXZ + ((float)Math.PI / 2F), directionY, pos, length, -1);
                return;
            }

            double var35 = x - this.chunkCenterX;
            double var37 = z - this.chunkCenterZ;
            double var39 = (double)(length - pos + 18) + (double)width;

            if (var35 * var35 + var37 * var37 > var39 * var39)
            {
                return;
            }

            double radiusW = 1.25D;
            float var33 = this.cosine(directionY);
            x += (double)(this.cosine(directionXZ) * var33);
            y += (double)this.sine(directionY);
            z += (double)(this.sine(directionXZ) * var33);

            if (isVerticalCave)
            {
                directionY *= 0.92F;
            }
            else
            {
                directionY *= 0.7F;
            }

            float radiusW_2;

            if (caveType < 2)
            {
                radiusW += (double)(this.sine((float)pos * (float)Math.PI / (float)length) * width);

                if (caveType >= 0)
                {
                    radiusW_2 = directionXZ - startDir;

                    if (radiusW_2 > ((float)Math.PI / 4F))
                    {
                        var23 = -0.5F;
                    }
                    else if (radiusW_2 < -((float)Math.PI / 4F))
                    {
                        var23 = 0.5F;
                    }

                    radiusW_2 = (float)(y - startY);

                    if (radiusW_2 > 5.0F)
                    {
                        var24 = -0.5F;
                    }
                    else if (radiusW_2 < -5.0F)
                    {
                        var24 = 0.5F;
                    }
                }
            }
            else
            {
                if (pos < length - 3)
                {
                    radiusW += 0.25D;
                }

                if (pos < length - 6)
                {
                    radiusW += (double)(width * this.caveRNG.nextFloat());
                }

                if (caveType == 2)
                {
                    radiusW_2 = (float)(y - startY);

                    if (radiusW_2 > 5.0F)
                    {
                        var24 = -0.5F;
                    }
                    else if (radiusW_2 < -5.0F)
                    {
                        var24 = 0.5F;
                    }
                }
                else if (caveType == 3)
                {
                    radiusW_2 = directionXZ - startDir;

                    if (radiusW_2 > ((float)Math.PI / 4F))
                    {
                        var23 = -0.5F;
                    }
                    else if (radiusW_2 < -((float)Math.PI / 4F))
                    {
                        var23 = 0.5F;
                    }
                }
            }

            directionY += var24 * 0.1F;
            var24 *= 0.9F;
            var24 += (this.caveRNG.nextFloat() - this.caveRNG.nextFloat()) * this.caveRNG.nextFloat() * 2.0F;

            if (this.caveRNG.nextInt(4) == 0)
            {
                radiusW = 1.25D;
            }

            directionXZ += var23 * curviness;
            var23 *= 0.75F;
            var23 += (this.caveRNG.nextFloat() - this.caveRNG.nextFloat()) * this.caveRNG.nextFloat() * 4.0F;
            double var38 = radiusW + 9.0D;

            if (x >= this.chunkCenterX - var38 && x <= this.chunkCenterX + var38 && z >= this.chunkCenterZ - var38 && z <= this.chunkCenterZ + var38)
            {
                double noiseMultiplier = 0.275D / Math.max(radiusW - 1.0D, 1.833333D);
                this.generateCaveSegment(x, y, z, radiusW, radiusW, noiseMultiplier, 1);
            }
        }
    }

    private void generateDirectionalCave(int x, int y, int z, int cx, int cz, int offset)
    {
        float direction = (this.rand.nextFloat() - 0.5F) * ((float)Math.PI / 4F);
        boolean length = false;
        cx = x - cx;
        cz = z - cz;

        if (cx > offset)
        {
            if (cz > offset)
            {
                direction -= 2.35619F;
            }
            else if (cz < -offset)
            {
                direction += 2.35619F;
            }
            else
            {
                direction += (float)Math.PI;
            }
        }
        else if (cx < -offset)
        {
            if (cz > offset)
            {
                direction -= ((float)Math.PI / 4F);
            }
            else if (cz < -offset)
            {
                direction += ((float)Math.PI / 4F);
            }
        }
        else if (cz > offset)
        {
            --direction;
        }
        else if (cz < -offset)
        {
            ++direction;
        }
        else
        {
            direction *= 8.0F;
            length = true;
        }

        int var9;

        if (!length)
        {
            var9 = this.rand.nextInt(16) + 8 + Math.round((float)Math.sqrt((double)(cx * cx + cz * cz)));
        }
        else
        {
            var9 = this.rand.nextInt(8) + 24;
        }

        this.generateHorizontalCave(this.rand.nextLong(), (double)x, (double)y, (double)z, this.rand.nextFloat() * 0.5F, direction, (this.rand.nextFloat() - 0.5F) / 4.0F, 0, var9, 0);
    }

    private void generateHorizontalLinkCave(int x, int y, int z, int cx, int cz)
    {
        float direction = (this.rand.nextFloat() - 0.5F) * ((float)Math.PI / 4F);
        cx = x - cx;
        cz = z - cz;

        if (cx >= 0)
        {
            if (cz >= 0)
            {
                direction += ((float)Math.PI / 4F);
            }
            else
            {
                direction -= ((float)Math.PI / 4F);
            }
        }
        else if (cz >= 0)
        {
            direction += 2.35619F;
        }
        else
        {
            direction -= 2.35619F;
        }

        int length = 128 + this.rand.nextInt(32);
        float width = this.rand.nextFloat() * 0.75F;
        this.generateHorizontalCave(this.rand.nextLong(), (double)x, (double)y, (double)z, width, direction, (this.rand.nextFloat() - 0.5F) / 4.0F, 0, length, 1);
    }

    private void generateVerticalCave(double x, int y1, int y2, double z)
    {
        this.generateVerticalCave(this.rand.nextLong(), x, y1, y2, z, -1.0F, 0.0D, 0.0D, 0);
    }

    private void generateVerticalCave(long seed, double x, int y1, int y2, double z, float width, double centerX, double centerZ, int maxDeviation)
    {
        this.caveRNG.setSeed(seed);
        float var23 = 0.0F;
        float directionXZ = this.caveRNG.nextFloat() * ((float)Math.PI * 2F);
        double startY = 0.0D;
        boolean descending = false;
        maxDeviation *= maxDeviation;

        if (y1 > y2)
        {
            int var35 = y1;
            y1 = y2;
            y2 = var35;
            startY = (double)(var35 + y1 - 1);
            descending = true;
        }

        for (; y1 < y2; ++y1)
        {
            double var34 = x - this.chunkCenterX;
            double var37 = z - this.chunkCenterZ;
            double var39 = (double)(y2 - y1 + 18) + (double)width;

            if (var34 * var34 + var37 * var37 > var39 * var39)
            {
                return;
            }

            double radiusW;

            if (width > 0.0F)
            {
                radiusW = (double)(1.5F + this.sine((float)y1 * ((float)Math.PI / 2F) / (float)y2) * width);
            }
            else if (width == 0.0F)
            {
                radiusW = 1.5D;
            }
            else
            {
                radiusW = (double)(1.5F + (this.caveRNG.nextFloat() + this.caveRNG.nextFloat()) * 0.5F);
            }

            x += (double)this.cosine(directionXZ);
            z += (double)this.sine(directionXZ);

            if (maxDeviation > 0)
            {
                float y = (float)(x - centerX);
                float devZ = (float)(z - centerZ);

                if (y * y + devZ * devZ > (float)maxDeviation)
                {
                    if (devZ >= 0.0F)
                    {
                        if (y >= 0.0F)
                        {
                            directionXZ = (directionXZ * 3.0F - 2.35619F) / 4.0F;
                        }
                        else
                        {
                            directionXZ = (directionXZ * 3.0F - ((float)Math.PI / 4F)) / 4.0F;
                        }
                    }
                    else if (y >= 0.0F)
                    {
                        directionXZ = (directionXZ * 3.0F + 2.35619F) / 4.0F;
                    }
                    else
                    {
                        directionXZ = (directionXZ * 3.0F + ((float)Math.PI / 4F)) / 4.0F;
                    }
                }
            }

            double var351 = descending ? startY - (double)y1 : (double)y1;
            directionXZ += var23 * 0.15F;
            var23 *= 0.75F;
            var23 += (this.caveRNG.nextFloat() - this.caveRNG.nextFloat()) * this.caveRNG.nextFloat() * 4.0F;
            double radiusW_2 = radiusW + 9.0D;

            if (x >= this.chunkCenterX - radiusW_2 && x <= this.chunkCenterX + radiusW_2 && z >= this.chunkCenterZ - radiusW_2 && z <= this.chunkCenterZ + radiusW_2)
            {
                double noiseMultiplier = 0.275D / Math.max(radiusW - 1.0D, 1.833333D);
                this.generateCaveSegment(x, var351, z, radiusW, 1.5D, noiseMultiplier, 1);
            }
        }

        if (!descending && width >= 0.5F)
        {
            this.generateCave(seed, x, (double)(y2 + 1), z, width - 0.5F, 0.0F, 0.0F, -1, -1, 1.0F);
        }
    }

    private void generateRavineCave(double x, double y, double z, float heightVariation)
    {
        int length = this.rand.nextInt(17) + 20;
        int segmentLength = Math.min(20, length + this.rand.nextInt(2) * 8 - 4);
        float width = this.rand.nextFloat() * this.rand.nextFloat() + 1.0F;
        float height = this.rand.nextFloat() * heightVariation + 2.0F;
        float direction = this.rand.nextFloat() * (float)Math.PI;
        float directionY = (this.rand.nextFloat() - 0.5F) / 4.0F;
        float slope = (this.rand.nextFloat() * 0.75F + 0.25F) * 0.25F * (float)(this.rand.nextInt(2) * 2 - 1);
        this.generateRavineCaveSegment(this.rand.nextLong(), x, y, z, width, direction, directionY, slope, segmentLength, height);
        segmentLength = Math.min(20, length + this.rand.nextInt(2) * 8 - 4);
        this.generateRavineCaveSegment(this.rand.nextLong(), x, y, z, width, direction + (float)Math.PI, -directionY, -slope, segmentLength, height);

        if (this.rand.nextBoolean())
        {
            length = this.rand.nextInt(17) + 20;
            segmentLength = Math.min(20, length + this.rand.nextInt(2) * 8 - 4);
            width = this.rand.nextFloat() * this.rand.nextFloat() + 1.0F;
            height = this.rand.nextFloat() * heightVariation + 2.0F;
            direction += (this.rand.nextFloat() * ((float)Math.PI / 4F) + 0.393699F) * (float)(this.rand.nextInt(2) * 2 - 1);
            directionY = (this.rand.nextFloat() - 0.5F) / 4.0F;
            slope = (this.rand.nextFloat() * 0.75F + 0.25F) * 0.25F * (float)(this.rand.nextInt(2) * 2 - 1) * ((float)this.rand.nextInt(2) * 0.75F + 0.25F);
            this.generateRavineCaveSegment(this.rand.nextLong(), x, y, z, width, direction, directionY, slope, segmentLength, height);

            if (this.rand.nextBoolean())
            {
                segmentLength = Math.min(20, length + this.rand.nextInt(2) * 8 - 4);
                this.generateRavineCaveSegment(this.rand.nextLong(), x, y, z, width, direction + (float)Math.PI, -directionY, -slope, segmentLength, height);
            }
        }
    }

    private void generateRavineCaveSegment(long seed, double x, double y, double z, float width, float directionXZ, float directionY, float slope, int length, float heightRatio)
    {
        this.caveRNG.setSeed(seed);
        float var24 = 0.0F;
        float var25 = 0.0F;
        float startDir = directionXZ;
        int end = width >= 1.666F ? 3 : (width >= 1.333F ? 2 : 1);
        width /= 2.0F;

        for (int pos = 0; pos < length; ++pos)
        {
            double var34 = x - this.chunkCenterX;
            double var36 = z - this.chunkCenterZ;
            double var38 = (double)(length - pos + 18);

            if (var34 * var34 + var36 * var36 > var38 * var38)
            {
                return;
            }

            double radiusW = (double)width;

            if (pos < length - end)
            {
                radiusW += (double)(this.caveRNG.nextFloat() * 0.5F);
            }

            if (pos < length - end * 2)
            {
                radiusW += (double)width;
            }

            double radiusH = radiusW * (double)heightRatio;
            radiusW *= (double)(this.caveRNG.nextFloat() * 0.25F + 0.75F);
            radiusH *= (double)(this.caveRNG.nextFloat() * 0.25F + 0.75F);
            float var32 = this.cosine(directionY);
            x += (double)(this.cosine(directionXZ) * var32);
            y += (double)(this.sine(directionY) + slope);
            z += (double)(this.sine(directionXZ) * var32);
            float dev = directionXZ - startDir;

            if (dev > 0.392699F)
            {
                var24 = -0.5F;
            }
            else if (dev < -0.392699F)
            {
                var24 = 0.5F;
            }

            directionY *= 0.7F;
            directionY += var25 * 0.1F;
            directionXZ += var24 * 0.1F;
            var25 *= 0.5F;
            var24 *= 0.75F;
            var25 += (this.caveRNG.nextFloat() - this.caveRNG.nextFloat()) * this.caveRNG.nextFloat() * 2.0F;
            var24 += (this.caveRNG.nextFloat() - this.caveRNG.nextFloat()) * this.caveRNG.nextFloat() * 4.0F;
            double radiusW_2 = radiusW + 9.0D;

            if (x >= this.chunkCenterX - radiusW_2 && x <= this.chunkCenterX + radiusW_2 && z >= this.chunkCenterZ - radiusW_2 && z <= this.chunkCenterZ + radiusW_2)
            {
                int var56 = MathHelper.floor(x - radiusW) - this.chunkX_16 - 1;
                int var35 = MathHelper.floor(x + radiusW) - this.chunkX_16 + 1;
                int var55 = (int)(y - radiusH) - 1;
                int var37 = (int)(y + radiusH) + 1;
                int var57 = MathHelper.floor(z - radiusW) - this.chunkZ_16 - 1;
                int var39 = MathHelper.floor(z + radiusW) - this.chunkZ_16 + 1;

                if (var56 < 0)
                {
                    var56 = 0;
                }

                if (var35 > 16)
                {
                    var35 = 16;
                }

                if (var55 < 1)
                {
                    var55 = 1;
                }

                if (var37 > 200)
                {
                    var37 = 200;
                }

                if (var57 < 0)
                {
                    var57 = 0;
                }

                if (var39 > 16)
                {
                    var39 = 16;
                }

                for (int var41 = var56; var41 < var35; ++var41)
                {
                    double var59 = ((double)(var41 + this.chunkX_16) + 0.5D - x) / radiusW;
                    var59 *= var59;

                    for (int var44 = var57; var44 < var39; ++var44)
                    {
                        double var45 = ((double)(var44 + this.chunkZ_16) + 0.5D - z) / radiusW;
                        var45 = var45 * var45 + var59;

                        if (var45 < 1.0D)
                        {
                            //int var47 = var41 << 12 | var44 << 8 | var37;

                            for (int var49 = var37 - 1; var49 >= var55; --var49)
                            {
                                int var47 = (var41 * 16 + var44) * 128 + var49;
                                double var50 = ((double)var49 + 0.5D - y) / radiusH;

                                if (this.chunkData[var47] != 0 && var50 > -0.7D && var45 + var50 * var50 / 6.0D + (double)(this.noiseGen.nextInt(3) - 1) * 0.3D < 1.0D)
                                {
                                    this.replaceBlock(var47, var41, var44);
                                }

                                --var47;
                            }
                        }
                    }
                }
            }
        }
    }

    private void generateMazeCaveSegment(long seed, int x, int y, int z, int direction, int length, float height)
    {
        this.caveRNG.setSeed(seed);
        float width = (direction & 1) == 1 ? 1.55F : 1.45F;

        for (int pos = 0; pos < length; ++pos)
        {
            double var34 = (double)x - this.chunkCenterX;
            double var36 = (double)z - this.chunkCenterZ;
            double var38 = (double)(length - pos + 18);

            if (var34 * var34 + var36 * var36 > var38 * var38)
            {
                return;
            }

            x = this.getOffsetX(x, direction, 1);
            z = this.getOffsetZ(z, direction, 1);
            double radiusW = (double)(this.caveRNG.nextFloat() * 0.5F + width);
            double radiusH = (double)(this.caveRNG.nextFloat() * 0.5F + height);
            double centerX = (double)(this.caveRNG.nextFloat() - 0.5F + (float)x);
            double var10000 = (double)(this.caveRNG.nextFloat() - 0.5F + (float)y);
            double centerZ = (double)(this.caveRNG.nextFloat() - 0.5F + (float)z);
            double radiusW_2 = radiusW + 9.0D;

            if (centerX >= this.chunkCenterX - radiusW_2 && centerX <= this.chunkCenterX + radiusW_2 && centerZ >= this.chunkCenterZ - radiusW_2 && centerZ <= this.chunkCenterZ + radiusW_2)
            {
                this.generateCaveSegment((double)x, (double)y, (double)z, radiusW, radiusH, (double)(width / 7.25F), 0);
            }
        }
    }

    private void generateRavines(int chunkX, int chunkZ, boolean flag, boolean largeRavines)
    {
        if (this.rand.nextInt(20) == 15)
        {
            byte bigRavine = 0;

            if (largeRavines)
            {
                int x = chunkX + this.caveOffsetX + 4;
                int offsetZ = chunkZ + this.caveOffsetZ + 4;

                if ((x & 7) == 0 && (offsetZ & 7) == 0 && (x & 8) != (offsetZ & 8))
                {
                    bigRavine = 2;
                }
                else if (this.rand.nextInt(25) < 19 && x % 3 == 0 && offsetZ % 3 == 0 && (x / 3 & 1) == (offsetZ / 3 & 1))
                {
                    bigRavine = 1;
                }
            }

            if (bigRavine > 0 || flag && this.rand.nextInt(30) < 11)
            {
                double var19 = (double)(chunkX * 16 + 8);
                double y = (double)(this.rand.nextInt(this.rand.nextInt(50) + 8) + 13);
                double z = (double)(chunkZ * 16 + 8);
                float directionXZ = this.rand.nextFloat() * (float)Math.PI;
                float directionY = (this.rand.nextFloat() - 0.5F) / 4.0F;
                float width = this.rand.nextFloat() * 4.0F + this.rand.nextFloat() * 2.0F;

                if (this.rand.nextInt(4) == 0)
                {
                    width += this.rand.nextFloat() * (bigRavine != 1 && width < 2.0F ? 0.0F : 2.0F);
                }

                float heightRatio = 3.0F;
                int length = 112 - this.rand.nextInt(15) * 2;
                float curviness = 0.05F;

                if (this.rand.nextInt(3) == 0)
                {
                    if (this.rand.nextInt(3) == 0)
                    {
                        curviness = 0.1F;
                    }
                    else
                    {
                        curviness = 0.075F;
                    }
                }

                if (bigRavine == 0)
                {
                    //int add = biomeList[this.world.getWorldChunkManager().getBiomeGenAt(chunkX * 16 + 8, chunkZ * 16 + 8).biomeID] >> 4;
                    int add = 0;

                    BiomeBase biomebase = this.world.getWorldChunkManager().getBiome(chunkX * 16 + 8, chunkZ * 16 + 8);

                    if (biomebase == BiomeBase.RAINFOREST || biomebase == BiomeBase.SEASONAL_FOREST || biomebase == BiomeBase.FOREST || biomebase == BiomeBase.TAIGA || biomebase == BiomeBase.PLAINS) {
                        add = 1;
                    } else if (biomebase == BiomeBase.SWAMPLAND || biomebase == BiomeBase.SAVANNA || biomebase == BiomeBase.SHRUBLAND) {
                        add = 2;
                    }

                    if (this.rand.nextBoolean() && add == 1 || add == 2)
                    {
                        add = this.rand.nextInt(2) + 1;

                        if (y < 31.5D)
                        {
                            y += (double)(add * 8);
                        }

                        heightRatio += (float)add;

                        if (width > (float)(6 - add))
                        {
                            width /= 2.0F;
                        }
                    }
                }
                else
                {
                    length += this.rand.nextInt(64) * 2;

                    if (width < 2.0F)
                    {
                        ++width;

                        if (width < 2.0F)
                        {
                            ++width;
                        }
                    }

                    width *= this.rand.nextFloat() * this.rand.nextFloat() * 1.5F + 1.0F;

                    if (bigRavine == 2)
                    {
                        length += 80 + this.rand.nextInt(40) * 2;
                        width += this.rand.nextFloat() * (float)(length / 56) + 3.0F;
                    }

                    if (length > 336)
                    {
                        length = 336;
                    }

                    if (width > 18.0F)
                    {
                        width = 18.0F;
                    }

                    if (y < 23.5D)
                    {
                        y += (double)(width / 1.5F);
                    }
                    else if (y > 52.5D)
                    {
                        y -= (double)(width * 1.5F);
                    }
                    else if (y > 42.5D)
                    {
                        y -= (double)(width / 1.5F);
                    }

                    curviness = (curviness + (float)length / 8960.0F + 0.0125F) / 1.5F;
                }

                this.generateRavine(var19, y, z, width, directionXZ, directionY, heightRatio, length, curviness, bigRavine > 0);
            }
        }
    }

    private void generateRavine(double x, double y, double z, float width, float directionXZ, float directionY, float heightRatio, int length, float curviness, boolean bigRavine)
    {
        float data = 1.0F;
        float ravineDataMultiplier = 1.1F - (width - 2.0F) * 0.07F;

        if (ravineDataMultiplier < 0.6F)
        {
            ravineDataMultiplier = 0.6F - (0.6F - ravineDataMultiplier) * 0.290322F;
        }

        int skipCount = 999;

        for (int i = 0; i < 128; ++i)
        {
            ++skipCount;

            if (skipCount >= 2 && (skipCount >= 5 || this.rand.nextInt(3) == 0))
            {
                skipCount = 0;
                data = (1.0F + this.rand.nextFloat() * this.rand.nextFloat() * ravineDataMultiplier) * (0.95F + (float)this.rand.nextInt(2) * 0.1F);
                data *= data;
            }

            this.ravineData[i] = data;
        }

        length /= 2;
        this.generateRavineHalf(this.rand.nextLong(), x, y, z, width, directionXZ, directionY, heightRatio, length, curviness, bigRavine);
        this.generateRavineHalf(this.rand.nextLong(), x, y, z, width, directionXZ + (float)Math.PI, -directionY, heightRatio, length, curviness, bigRavine);
    }

    private void generateRavineHalf(long seed, double x, double y, double z, float width, float directionXZ, float directionY, float heightRatio, int length, float curviness, boolean bigRavine)
    {
        this.caveRNG.setSeed(seed);
        float var24 = 0.0F;
        float var25 = 0.0F;
        float heightMultiplier = 1.0F;
        float startDir = directionXZ;

        for (int pos = 0; pos < length; ++pos)
        {
            double radiusW = (double)(1.5F + this.cosine((float)pos * ((float)Math.PI / 2F) / (float)length) * width);
            double var34 = x - this.chunkCenterX;
            double var36 = z - this.chunkCenterZ;
            double var38 = (double)(length - pos + 18) + radiusW;

            if (var34 * var34 + var36 * var36 > var38 * var38)
            {
                return;
            }

            if (width > 2.0F)
            {
                if (bigRavine)
                {
                    if (radiusW < 5.0D)
                    {
                        heightMultiplier = 1.71428F - (float)radiusW / 7.0F;
                    }
                    else if (radiusW < 11.0D)
                    {
                        heightMultiplier = 1.2F - (float)radiusW / 25.0F;
                    }
                    else
                    {
                        heightMultiplier = 0.96706F - (float)radiusW / 53.125F;
                    }
                }
                else
                {
                    heightMultiplier = 1.875F - (float)radiusW / 4.0F;

                    if (heightMultiplier < 1.0F)
                    {
                        heightMultiplier = 1.0F;
                    }
                }
            }

            double radiusH = radiusW * (double)(heightMultiplier * heightRatio);
            radiusW *= (double)(this.caveRNG.nextFloat() * 0.25F + 0.75F);
            radiusH *= (double)(this.caveRNG.nextFloat() * 0.25F + 0.75F);

            if (this.caveRNG.nextInt(4) == 0)
            {
                radiusW = radiusW / 5.0D + 0.5D;
                radiusH = radiusH / 4.0D + 1.5D;
            }

            float var32 = this.cosine(directionY);
            x += (double)(this.cosine(directionXZ) * var32);
            y += (double)this.sine(directionY);
            z += (double)(this.sine(directionXZ) * var32);
            float dev = directionXZ - startDir;

            if (dev > ((float)Math.PI / 4F))
            {
                var24 = -0.5F;
            }
            else if (dev < -((float)Math.PI / 4F))
            {
                var24 = 0.5F;
            }

            directionY *= 0.7F;
            directionY += var25 * curviness;
            directionXZ += var24 * curviness;
            var25 *= 0.8F;
            var24 *= 0.5F;
            var25 += (this.caveRNG.nextFloat() - this.caveRNG.nextFloat()) * this.caveRNG.nextFloat() * 2.0F;
            var24 += (this.caveRNG.nextFloat() - this.caveRNG.nextFloat()) * this.caveRNG.nextFloat() * 4.0F;
            double radiusW_2 = radiusW + 9.0D;

            if (x >= this.chunkCenterX - radiusW_2 && x <= this.chunkCenterX + radiusW_2 && z >= this.chunkCenterZ - radiusW_2 && z <= this.chunkCenterZ + radiusW_2)
            {
                int var56 = MathHelper.floor(x - radiusW) - this.chunkX_16 - 1;
                int var35 = MathHelper.floor(x + radiusW) - this.chunkX_16 + 1;
                int var55 = (int)(y - radiusH) - 1;
                int var37 = (int)(y + radiusH) + 1;
                int var57 = MathHelper.floor(z - radiusW) - this.chunkZ_16 - 1;
                int var39 = MathHelper.floor(z + radiusW) - this.chunkZ_16 + 1;

                if (var56 < 0)
                {
                    var56 = 0;
                }

                if (var35 > 16)
                {
                    var35 = 16;
                }

                if (var55 < 1)
                {
                    var55 = 1;
                }

                if (var37 > 120)
                {
                    var37 = 120;
                }

                if (var57 < 0)
                {
                    var57 = 0;
                }

                if (var39 > 16)
                {
                    var39 = 16;
                }

                double noiseMultiplier = 0.33333333D / Math.max(radiusW - 0.5D, 2.5D);

                for (int var41 = var56; var41 < var35; ++var41)
                {
                    double var59 = ((double)(var41 + this.chunkX_16) + 0.5D - x) / radiusW;
                    var59 *= var59;

                    for (int var44 = var57; var44 < var39; ++var44)
                    {
                        double var45 = ((double)(var44 + this.chunkZ_16) + 0.5D - z) / radiusW;
                        var45 = var45 * var45 + var59;

                        if (var45 < 1.0D)
                        {
                            //int var47 = var41 << 12 | var44 << 8 | var37;

                            for (int var49 = var37 - 1; var49 >= var55; --var49)
                            {
                                int var47 = (var41 * 16 + var44) * 128 + var49;
                                double var50 = ((double)var49 + 0.5D - y) / radiusH;

                                if (this.chunkData[var47] != 0 && var45 * (double)this.ravineData[var49] + var50 * var50 / 6.0D + (double)(this.noiseGen.nextInt(3) - 1) * noiseMultiplier < 1.0D)
                                {
                                    this.replaceBlock(var47, var41, var44);
                                }

                                --var47;
                            }
                        }
                    }
                }
            }
        }
    }

    private void generateCaveSegment(double x, double y, double z, double radiusW, double radiusH, double noiseMultiplier, int noiseOffset)
    {
        int var55 = MathHelper.floor(x - radiusW) - this.chunkX_16 - 1;
        int var36 = MathHelper.floor(x + radiusW) - this.chunkX_16 + 1;
        int var57 = (int)(y - radiusH) - 1;
        int var38 = (int)(y + radiusH) + 1;
        int var56 = MathHelper.floor(z - radiusW) - this.chunkZ_16 - 1;
        int var40 = MathHelper.floor(z + radiusW) - this.chunkZ_16 + 1;

        if (var55 < 0)
        {
            var55 = 0;
        }

        if (var36 > 16)
        {
            var36 = 16;
        }

        if (var57 < 1)
        {
            var57 = 1;
        }

        if (var38 > 200)
        {
            var38 = 200;
        }

        if (var56 < 0)
        {
            var56 = 0;
        }

        if (var40 > 16)
        {
            var40 = 16;
        }

        for (int var42 = var55; var42 < var36; ++var42)
        {
            double var59 = ((double)(var42 + this.chunkX_16) + 0.5D - x) / radiusW;
            var59 *= var59;

            for (int var45 = var56; var45 < var40; ++var45)
            {
                double var46 = ((double)(var45 + this.chunkZ_16) + 0.5D - z) / radiusW;
                var46 = var46 * var46 + var59;

                if (var46 < 1.0D)
                {
                    //int var48 = var42 << 12 | var45 << 8 | var38;

                    for (int var50 = var38 - 1; var50 >= var57; --var50)
                    {
                        int var48 = (var42 * 16 + var45) * 128 + var50;
                        double var51 = ((double)var50 + 0.5D - y) / radiusH;

                        if (this.chunkData[var48] != 0 && var51 > -0.7D && var51 * var51 + var46 + (double)(this.noiseGen.nextInt(3) - noiseOffset) * noiseMultiplier < 1.0D)
                        {
                            this.replaceBlock(var48, var42, var45);
                        }

                        --var48;
                    }
                }
            }
        }
    }

    private void replaceBlock(int index, int x, int z)
    {
        int y = index & 127;
        int b;
        boolean flag1 = false;

        if (y >= 40 && y <= 64)
        {
            int minX = Math.max(x - 1, 0);
            int maxX = Math.min(x + 1, 15);
            int minZ = Math.max(z - 1, 0);
            int maxZ = Math.min(z + 1, 15);
            int data;

            for (data = minX; data <= maxX; ++data)
            {
                for (b = minZ; b <= maxZ; ++b)
                {
                    //int xyz = data << 12 | b << 8 | y;
                    int xyz = (data * 16 + b) * 128 + y;

                    if (this.chunkData[xyz] == Block.STATIONARY_WATER.id)
                    {
                        return;
                    }

                    if (this.chunkData[xyz + 1] == Block.STATIONARY_WATER.id)
                    {
                        return;
                    }
                }
            }

            for (data = minX; data <= maxX; ++data)
            {
                b = z - 2;

                if (b >= 0 && this.chunkData[(data * 16 + b) * 128 + y] == Block.STATIONARY_WATER.id)
                {
                    return;
                }

                b = z + 2;

                if (b <= 15 && this.chunkData[(data * 16 + b) * 128 + y] == Block.STATIONARY_WATER.id)
                {
                    return;
                }
            }

            for (b = minZ; b <= maxZ; ++b)
            {
                data = x - 2;

                if (data >= 0 && this.chunkData[(data * 16 + b) * 128 + y] == Block.STATIONARY_WATER.id)
                {
                    return;
                }

                data = x + 2;

                if (data <= 15 && this.chunkData[(data * 16 + b) * 128 + y] == Block.STATIONARY_WATER.id)
                {
                    return;
                }
            }

            /*if (this.chunkData[x << 12 | z << 8 | y + 2] == Block.STATIONARY_WATER.id)
            {
                return;
            }*/
        }

        if (this.chunkData[index] == Block.GRASS.id)
        {
            flag1 = true;
        }

        if (this.chunkData[index] == Block.STONE.id || this.chunkData[index] == Block.DIRT.id || this.chunkData[index] == Block.GRASS.id)
        {
            if (y < 11)
            {
                this.chunkData[index] = (byte) Block.STATIONARY_LAVA.id;
            }
            else {
                this.chunkData[index] = 0;

                if (y >= 40)
                {
                    byte var13 = this.chunkData[index + 1];

                    if (var13 == Block.SAND.id) {
                        this.chunkData[index + 1] = (byte) Block.SANDSTONE.id;
                    } else if (var13 == Block.GRAVEL.id) {
                        this.chunkData[index + 1] = (byte) Block.STONE.id;
                    }

                    if (flag1 && this.chunkData[index - 1] == Block.DIRT.id)
                    {
                        this.chunkData[index - 1] = (byte) Block.GRASS.id;
                    }
                }
            }
        }
    }

    private void initializeCaveData()
    {
        boolean flag = Math.abs(this.chunk_X) < 82 && Math.abs(this.chunk_Z) < 82;
        int distance = 6724;

        for (int z = -18; z <= 18; ++z)
        {
            int zIndex = (z + 18) * 37 + 18;
            int cz = this.chunk_Z + z;
            int z2 = z * z;

            for (int x = -18; x <= 18; ++x)
            {
                int x2z2 = x * x + z2;

                if (x2z2 <= 329)
                {
                    int cx = this.chunk_X + x;

                    if (flag)
                    {
                        distance = cx * cx + cz * cz;
                    }

                    byte data = 0;

                    if (this.validColossalCaveLocation(cx, cz, distance))
                    {
                        data = -1;
                    }
                    else if (x2z2 <= 287)
                    {
                        if (this.validRegionalCaveLocation(cx, cz, distance))
                        {
                            data = 2;
                        }
                        else if (x2z2 <= 262)
                        {
                            int d = this.validSpecialCaveLocation(cx, cz, distance);

                            if (d > 0)
                            {
                                data = (byte)d;
                            }
                        }
                    }

                    this.caveDataArray[zIndex + x] = data;
                }
            }
        }
    }

    private int validCaveLocation(int cx, int cz)
    {
        byte flag = 1;

        for (int z = -6; z <= 6; ++z)
        {
            int zIndex = (cz + z + 18) * 37 + cx + 18;
            int z2 = z * z;

            for (int x = -6; x <= 6; ++x)
            {
                int x2z2 = x * x + z2;

                if (x2z2 <= 37)
                {
                    byte data = this.caveDataArray[zIndex + x];

                    if (data != 0)
                    {
                        if (data == -1)
                        {
                            if (x2z2 == 0)
                            {
                                return -1;
                            }

                            return 0;
                        }

                        if (data == 1 && x2z2 <= 17)
                        {
                            if (x2z2 == 0)
                            {
                                return 2;
                            }

                            if (x2z2 <= 5)
                            {
                                return 0;
                            }

                            flag = 5;
                        }

                        if (data == 4 && x2z2 <= 17)
                        {
                            if (x2z2 == 0)
                            {
                                return 6;
                            }

                            if (x2z2 <= 5)
                            {
                                return 0;
                            }

                            flag = 5;
                        }

                        if (data == 5 && x2z2 <= 17)
                        {
                            if (x2z2 == 0)
                            {
                                return 7;
                            }

                            if (x2z2 <= 5)
                            {
                                return 0;
                            }

                            flag = 5;
                        }

                        if (data == 2 && x2z2 <= 24)
                        {
                            if (x2z2 == 0)
                            {
                                return 3;
                            }

                            if (flag == 1)
                            {
                                flag = 4;
                            }
                        }

                        if (data == 3)
                        {
                            if (x2z2 <= 10)
                            {
                                return 0;
                            }

                            flag = 5;
                        }
                    }
                }
            }
        }

        return flag;
    }

    public boolean validColossalCaveLocation(int chunkX, int chunkZ, int distance)
    {
        chunkX += this.caveOffsetX;
        chunkZ += this.caveOffsetZ;

        if ((chunkX & 64) == (chunkZ & 64))
        {
            return false;
        }
        else
        {
            this.caveRNG.setSeed(((long)(chunkX / 64) * 341873128712L + (long)(chunkZ / 64) * 132897987541L) * this.seedMultiplier);
            return (chunkX & 63) == this.caveRNG.nextInt(32) && (chunkZ & 63) == this.caveRNG.nextInt(32);
        }
    }

    public int validSpecialCaveLocation(int chunkX, int chunkZ, int distance)
    {
        int offsetX = chunkX + this.caveOffsetX + 1;
        int offsetZ = chunkZ + this.caveOffsetZ + 1;

        if ((offsetX & 7) <= 2 && (offsetZ & 7) <= 2)
        {
            int d = this.validSpecialCaveLocation2(offsetX, offsetZ);

            if (d != 0)
            {
                return d;
            }

            offsetX -= 16;
            offsetZ -= 16;
            this.caveRNG.setSeed(((long)(offsetX / 32) * 341873128712L + (long)(offsetZ / 32) * 132897987541L) * this.seedMultiplier);

            if ((offsetX & 31) == this.caveRNG.nextInt(4) * 8 + this.caveRNG.nextInt(3) && (offsetZ & 31) == this.caveRNG.nextInt(4) * 8 + this.caveRNG.nextInt(3))
            {
                boolean flag = distance < 5041;

                for (int z = -7; z <= 7; ++z)
                {
                    int cz = chunkZ + z;

                    for (int x = -7; x <= 7; ++x)
                    {
                        int x2z2 = x * x + z * z;

                        if (x2z2 <= 50)
                        {
                            int cx = chunkX + x;

                            if (flag)
                            {
                                distance = cx * cx + cz * cz;
                            }

                            if (this.validColossalCaveLocation(cx, cz, distance))
                            {
                                return 0;
                            }

                            if (x2z2 <= 37)
                            {
                                if (x2z2 <= 24)
                                {
                                    if (this.validRegionalCaveLocation(cx, cz, distance))
                                    {
                                        return 0;
                                    }

                                    if (x2z2 > 0 && x2z2 <= 17 && this.validSpecialCaveLocation2(cx + this.caveOffsetX + 1, cz + this.caveOffsetZ + 1) != 0)
                                    {
                                        return 0;
                                    }
                                }
                            }
                        }
                    }
                }

                return 1;
            }
        }

        return 0;
    }

    private int validSpecialCaveLocation2(int offsetX, int offsetZ)
    {
        this.caveRNG.setSeed(((long)(offsetX / 64) * 341873128712L + (long)(offsetZ / 64) * 132897987541L) * this.seedMultiplier);
        int x;
        int z;

        if (this.caveRNG.nextBoolean())
        {
            x = this.caveRNG.nextInt(4) * 8 + this.caveRNG.nextInt(3);
            z = this.caveRNG.nextInt(3) * 8 + this.caveRNG.nextInt(3) + 40;
        }
        else
        {
            x = this.caveRNG.nextInt(3) * 8 + this.caveRNG.nextInt(3) + 40;
            z = this.caveRNG.nextInt(4) * 8 + this.caveRNG.nextInt(3);
        }

        return (offsetX & 63) == x && (offsetZ & 63) == z ? 4 : ((offsetX & 63) == this.caveRNG.nextInt(3) * 8 + this.caveRNG.nextInt(3) + 40 && (offsetZ & 63) == this.caveRNG.nextInt(3) * 8 + this.caveRNG.nextInt(3) + 40 ? 5 : 0);
    }

    public boolean validRegionalCaveLocation(int chunkX, int chunkZ, int distance)
    {
        chunkX += this.caveOffsetX;
        chunkZ += this.caveOffsetZ;
        this.caveRNG.setSeed(((long)(chunkX / 64) * 341873128712L + (long)(chunkZ / 64) * 132897987541L) * this.seedMultiplier);
        chunkX &= 63;
        chunkZ &= 63;
        int offsetX;
        int offsetZ;

        if (this.caveRNG.nextBoolean())
        {
            offsetX = this.caveRNG.nextInt(9) + 38;
            offsetZ = this.caveRNG.nextInt(21);
        }
        else
        {
            offsetX = this.caveRNG.nextInt(21);
            offsetZ = this.caveRNG.nextInt(9) + 38;
        }

        return chunkX >= offsetX && chunkX <= offsetX + 11 && chunkZ >= offsetZ && chunkZ <= offsetZ + 11;
    }

    public boolean isGiantCaveRegion(int chunkX, int chunkZ)
    {
        chunkX = (chunkX + this.caveOffsetX) / 64;
        chunkZ = (chunkZ + this.caveOffsetZ) / 64;
        this.caveRNG.setSeed(((long)(chunkX / 2) * 341873128712L + (long)(chunkZ / 2) * 132897987541L) * this.seedMultiplier);
        return (chunkX & 1) == this.caveRNG.nextInt(2) && (chunkZ & 1) == this.caveRNG.nextInt(2);
    }

    private int getQuadrantX(int i)
    {
        return (i + 1 & 3) < 2 ? -1 : 1;
    }

    private int getQuadrantZ(int i)
    {
        return (i & 3) < 2 ? -1 : 1;
    }

    private int getOffsetX(int x, int direction, int offset)
    {
        switch (direction & 7)
        {
            case 0:
            case 1:
            case 7:
                return x + offset;

            case 2:
            case 6:
            default:
                return x;

            case 3:
            case 4:
            case 5:
                return x - offset;
        }
    }

    private int getOffsetZ(int z, int direction, int offset)
    {
        switch (direction & 7)
        {
            case 1:
            case 2:
            case 3:
                return z + offset;

            case 4:
            default:
                return z;

            case 5:
            case 6:
            case 7:
                return z - offset;
        }
    }

    private float sine(float f)
    {
        return SINE_TABLE[(int)(f * 162.97466F) & 1023];
    }

    private float cosine(float f)
    {
        return SINE_TABLE[(int)(f * 162.97466F) + 256 & 1023];
    }

    static
    {
        for (int i = 0; i < 1024; ++i)
        {
            SINE_TABLE[i] = (float)Math.sin((double)i * Math.PI * 2.0D / 1024.0D);
        }
    }
}
