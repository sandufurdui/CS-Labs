using System;
using System.Collections.Generic;
using System.IO;
using System.Security.Cryptography;
using System.Text;
using MongoDB.Bson;
using MongoDB.Driver;

namespace MongoDB
{
    public class MongoDBController
    {
        public static string encryptedString2 = "";
        public static void updateDBWithEncryptedValue(string dbField, List<BsonDocument> filtersResult, IMongoCollection<BsonDocument> collection)
        {

            foreach (var doc in filtersResult)
            {


                var value = doc.GetValue(dbField);

                var thisFilter = Builders<BsonDocument>.Filter.Eq("_id", doc.GetValue("_id"));

                Console.WriteLine("Value: " + value.ToString());
                Console.WriteLine("\n------\n");
                string encryptedString = Encrypt(value.ToString());
                encryptedString2 = Encrypt(encryptedString);
                Console.WriteLine("ENCRYPTED STRING: " + encryptedString2);
                Console.WriteLine("\n------\n");

                var update = Builders<BsonDocument>.Update.Set(dbField, encryptedString2);

                collection.UpdateOne(thisFilter, update);
            }
        }

        public static void updateDBWithDecryptedValue(string dbField, List<BsonDocument> filtersResult, IMongoCollection<BsonDocument> collection)
        {

            foreach (var doc in filtersResult)
            {
                var value = doc.GetValue(dbField);

                var thisFilter = Builders<BsonDocument>.Filter.Eq("_id", doc.GetValue("_id"));

                string decryptedString = Decrypt(encryptedString2);
                string decryptedString2 = Decrypt(decryptedString);
                Console.WriteLine("DECRYPTED STRING: " + decryptedString2);
                Console.WriteLine("\n------\n");
                var update = Builders<BsonDocument>.Update.Set(dbField, decryptedString2);

                collection.UpdateOne(thisFilter, update);
            }
        }

        public static string Encrypt(string text)
        {
            string EncryptionKey = "EncryptionKey";
            byte[] clearBytes = Encoding.Unicode.GetBytes(text);
            using (Aes encryptor = Aes.Create())
            {
                Rfc2898DeriveBytes pdb = new Rfc2898DeriveBytes(EncryptionKey, new byte[] { 0x49, 0x76, 0x61, 0x6e, 0x20, 0x4d, 0x65, 0x64, 0x76, 0x65, 0x64, 0x65, 0x76 });
                encryptor.Key = pdb.GetBytes(32);
                encryptor.IV = pdb.GetBytes(16);
                using (MemoryStream ms = new MemoryStream())
                {
                    using (CryptoStream cs = new CryptoStream(ms, encryptor.CreateEncryptor(), CryptoStreamMode.Write))
                    {
                        cs.Write(clearBytes, 0, clearBytes.Length);
                        cs.Close();
                    }
                    text = Convert.ToBase64String(ms.ToArray());
                }
            }
            return text;
        }

        public static string Decrypt(string text)
        {
            string EncryptionKey = "EncryptionKey";
            text = text.Replace(" ", "+");
            byte[] cipherBytes = Convert.FromBase64String(text);
            using (Aes encryptor = Aes.Create())
            {
                Rfc2898DeriveBytes pdb = new Rfc2898DeriveBytes(EncryptionKey, new byte[] { 0x49, 0x76, 0x61, 0x6e, 0x20, 0x4d, 0x65, 0x64, 0x76, 0x65, 0x64, 0x65, 0x76 });
                encryptor.Key = pdb.GetBytes(32);
                encryptor.IV = pdb.GetBytes(16);
                using (MemoryStream ms = new MemoryStream())
                {
                    using (CryptoStream cs = new CryptoStream(ms, encryptor.CreateDecryptor(), CryptoStreamMode.Write))
                    {
                        cs.Write(cipherBytes, 0, cipherBytes.Length);
                        cs.Close();
                    }
                    text = Encoding.Unicode.GetString(ms.ToArray());
                }
            }
            return text;
        }
    }
}
