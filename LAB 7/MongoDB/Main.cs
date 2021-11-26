using System;
using System.IO;
using System.Security.Cryptography;
using System.Text;
using MongoDB.Bson;
using MongoDB.Driver;

using System.Collections.Generic;
using System.Linq;

namespace MongoDB
{
    class Program
    {
        public static string randomWord = "";
        static void Main(string[] args)
        {
            MongoClient dbClient = new MongoClient("mongodb://furdui:Password1@cluster0-shard-00-00.184so.mongodb.net:27017,cluster0-shard-00-01.184so.mongodb.net:27017,cluster0-shard-00-02.184so.mongodb.net:27017/MongoDB?ssl=true&replicaSet=atlas-opxi80-shard-0&authSource=admin&retryWrites=true&w=majority");
            var database = dbClient.GetDatabase("MongoDB");

            var collection = database.GetCollection<BsonDocument>("MongoDB");

            var document = new BsonDocument();


            BsonDocument firstStudent =
        new BsonDocument { { "University", "UTM" }, { "fullName", "Name Surname" }, { "email", " test1@gmail.com" }, { "dateBirth", "01/01/2000" }, { "sudentGroup", "FAF-192" } }
             ;


            BsonDocument secondStudent =
        new BsonDocument { { "University", "UTM" }, { "fullName", "Name Surname" }, { "email", " test2@gmail.com" }, { "dateBirth", "01/01/2000" }, { "sudentGroup", "FAF-192" } }
             ;


            BsonDocument thirdStudent =
        new BsonDocument { { "University", "UTM" }, { "fullName", "Name Surname" }, { "email", " test3@gmail.com" }, { "dateBirth", "01/01/2000" }, { "sudentGroup", "FAF-192" } }
             ;


            database.DropCollection("students");

            collection.InsertMany(new List<BsonDocument> { firstStudent, secondStudent, thirdStudent });

            Console.WriteLine(document.ToString());
            var filter = Builders<BsonDocument>.Filter.Eq("University", "UTM");
            var result = collection.Find(filter).ToList();

            MongoDBController.updateDBWithEncryptedValue("dateBirth", result, collection);

            //MongoDBController.updateDBWithDecryptedValue("dateBirth", result, collection);

        }
    }

}





